package com.tonywww.slashblade_sendims.entities.boss;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 暮刃守卫的招式。每个招式是一段按 tick 推进的脚本：{@link Attack#step} 每 tick 调用一次，
 * 返回 false 表示结束。前摇统一用 {@link Attack#windup}，由守卫负责 Leader 粒子环与招架窗口。
 */
final class WardenAttacks {
    // 前摇（tick）
    static final int SHORT = 10;
    static final int MEDIUM = 20;
    static final int BLUE_LONG = 30;
    static final int RED_MEDIUM = 26;
    static final int RED_LONG = 36;
    static final int RED_FINAL = 40;
    static final int SUB_WINDUP = 6;
    // 伤害倍率（× 攻击力）
    static final float MEDIUM_DAMAGE = 1.0F;
    static final float HIGH_DAMAGE = 1.2F;
    static final float MEDIUM_LOW_DAMAGE = 0.75F;
    // 范围半径（格）
    static final double SMALL = 3.5D;
    static final double MEDIUM_RADIUS = 5.5D;
    static final double LARGE = 9.0D;
    static final double HUGE = 14.0D;

    private WardenAttacks() {
    }

    /** 招式与释放距离（到目标的水平距离）。 */
    enum Move {
        ENGETSU(false, 0.0D, 5.0D),
        OUGIGAESHI(false, 0.0D, 5.0D),
        SHUKUCHI(false, 3.0D, 11.0D),
        NANATSU_SHIGURE(false, 0.0D, 11.0D),
        RYUSEI_OTOSHI(false, 11.0D, 32.0D),
        SHUEN_ICHIMONJI(true, 0.0D, 9.0D),
        SHORYUSEN(true, 3.0D, 11.0D),
        SANJU_FUJIN(true, 0.0D, 13.0D);

        final boolean red;
        final double minDistance;
        final double maxDistance;

        Move(boolean red, double minDistance, double maxDistance) {
            this.red = red;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }

        boolean inRange(double distance) {
            return distance >= minDistance && distance <= maxDistance;
        }

        Attack create() {
            return switch (this) {
                case ENGETSU -> new Engetsu();
                case OUGIGAESHI -> new Ougigaeshi();
                case SHUKUCHI -> new Shukuchi();
                case NANATSU_SHIGURE -> new NanatsuShigure();
                case RYUSEI_OTOSHI -> new RyuseiOtoshi();
                case SHUEN_ICHIMONJI -> new ShuenIchimonji();
                case SHORYUSEN -> new Shoryusen();
                case SANJU_FUJIN -> new SanjuFujin();
            };
        }
    }

    abstract static class Attack {
        final Move move;
        int t = -1;

        Attack(Move move) {
            this.move = move;
        }

        final boolean step(TwilightBladeWarden warden) {
            t++;
            return tick(warden);
        }

        abstract boolean tick(TwilightBladeWarden warden);

        /** 结束后立即衔接的招式（不经过出招间隔、不再播放教程提示），没有则为 null。 */
        Attack next(TwilightBladeWarden warden) {
            return null;
        }

        /** 招式结束或被打断时清理。 */
        void stop(TwilightBladeWarden warden) {
            warden.clearTelegraph();
            warden.setNoGravity(false);
        }

        /**
         * 从第 at tick 开始一段 length tick 的前摇，返回值表示当前 tick 是否为出招点。
         * track 为 true 时前摇期间持续转向目标。
         */
        boolean windup(TwilightBladeWarden warden, int at, int length, boolean track) {
            if (t == at) {
                warden.telegraph(move.red ? TelegraphKind.RED : TelegraphKind.BLUE, length);
            }
            if (track && t >= at && t < at + length) {
                warden.faceTarget();
            }
            if (t == at + length) {
                warden.clearTelegraph();
                return true;
            }
            return false;
        }
    }

    // ===================== 蓝刀 =====================

    /** 圆月：短前摇，自身周围小范围一次中伤害。 */
    static final class Engetsu extends Attack {
        Engetsu() {
            super(Move.ENGETSU);
        }

        @Override
        boolean tick(TwilightBladeWarden warden) {
            if (windup(warden, 0, SHORT, true)) {
                WardenCombat.strikeCircle(warden, warden.position(), SMALL, MEDIUM_DAMAGE, false);
            }
            return t < SHORT + 4;
        }
    }

    /** 扇返：中前摇，前方 120° 扇形；紧接短前摇，自身周围中范围。 */
    static final class Ougigaeshi extends Attack {
        private static final double FAN_RADIUS = 6.0D;

        Ougigaeshi() {
            super(Move.OUGIGAESHI);
        }

        @Override
        boolean tick(TwilightBladeWarden warden) {
            if (windup(warden, 0, MEDIUM, true)) {
                WardenCombat.strikeSector(warden, FAN_RADIUS, 60.0D, MEDIUM_DAMAGE, false);
            }
            if (windup(warden, MEDIUM + 1, SHORT, false)) {
                WardenCombat.strikeCircle(warden, warden.position(), MEDIUM_RADIUS, MEDIUM_DAMAGE, false);
            }
            return t < MEDIUM + 1 + SHORT + 4;
        }
    }

    /** 缩地：中前摇，前摇过半时记下目标位置，冲到该位置前 2 格，撞到的敌人吃中伤害；随后接圆月或扇返。 */
    static final class Shukuchi extends Attack {
        private static final double SPEED = 1.4D;
        private static final double STOP_SHORT = 2.0D;
        private final Set<UUID> hit = new HashSet<>();
        private Vec3 aim;
        private Vec3 direction = Vec3.ZERO;
        private double distance;
        private int dashTicks = -1;

        Shukuchi() {
            super(Move.SHUKUCHI);
        }

        @Override
        boolean tick(TwilightBladeWarden warden) {
            if (t == MEDIUM / 2) {
                aim = warden.targetPosition();
            }
            if (aim != null && t < MEDIUM) {
                warden.faceTowards(aim);
            }
            if (windup(warden, 0, MEDIUM, t < MEDIUM / 2)) {
                if (aim == null) {
                    aim = warden.targetPosition();
                }
                Vec3 flat = new Vec3(aim.x - warden.getX(), 0.0D, aim.z - warden.getZ());
                distance = Math.max(0.0D, flat.length() - STOP_SHORT);
                direction = flat.lengthSqr() > 1.0E-4D ? flat.normalize() : Vec3.ZERO;
                dashTicks = (int) Math.ceil(distance / SPEED);
                warden.faceDirection(direction);
                WardenCombat.dashStart(warden, false);
            }
            if (dashTicks < 0 || t <= MEDIUM) {
                return true;
            }
            int dashTick = t - MEDIUM;
            if (dashTick <= dashTicks) {
                double step = Math.min(SPEED, distance - (dashTick - 1) * SPEED);
                boolean blocked = step > 0.0D && warden.moveBy(direction.scale(step));
                WardenCombat.hitContacts(warden, hit, MEDIUM_DAMAGE);
                WardenCombat.trail(warden, false);
                if (blocked) {
                    dashTicks = dashTick;
                }
            }
            return dashTick < dashTicks + 2;
        }

        @Override
        Attack next(TwilightBladeWarden warden) {
            return warden.getRandom().nextBoolean() ? new Engetsu() : new Ougigaeshi();
        }
    }

    /** 七时雨：中前摇后连斩 7 次（前方小扇形、中偏低伤害）；每斩后追向目标（有上限），再短前摇锁定方向出斩。 */
    static final class NanatsuShigure extends Attack {
        private static final int HITS = 7;
        private static final int CHASE_MAX = 12;
        private static final double CHASE_SPEED = 0.45D;
        private static final double CHASE_STOP = 2.5D;
        private static final double RADIUS = 4.0D;
        private int stage;
        private int stageStart;
        private int hits;
        private float lockedYaw;

        NanatsuShigure() {
            super(Move.NANATSU_SHIGURE);
        }

        @Override
        boolean tick(TwilightBladeWarden warden) {
            if (t == 0) {
                warden.telegraph(TelegraphKind.BLUE, MEDIUM);
            }
            int stageTick = t - stageStart;
            switch (stage) {
                case 0 -> {
                    warden.faceTarget();
                    if (stageTick >= MEDIUM) {
                        strike(warden);
                    }
                }
                case 1 -> {
                    warden.faceTarget();
                    boolean close = warden.chase(CHASE_SPEED, CHASE_STOP);
                    if (close || stageTick >= CHASE_MAX) {
                        stage = 2;
                        stageStart = t;
                        warden.faceTarget();
                        lockedYaw = warden.getYRot();
                        warden.telegraph(TelegraphKind.BLUE, SUB_WINDUP);
                    }
                }
                case 2 -> {
                    warden.setFacing(lockedYaw);
                    if (stageTick >= SUB_WINDUP) {
                        strike(warden);
                    }
                }
                default -> {
                    return stageTick < 6;
                }
            }
            return true;
        }

        private void strike(TwilightBladeWarden warden) {
            warden.clearTelegraph();
            WardenCombat.strikeSector(warden, RADIUS, 45.0D, MEDIUM_LOW_DAMAGE, false);
            hits++;
            stage = hits >= HITS ? 3 : 1;
            stageStart = t;
        }
    }

    /** 流星坠：中前摇后跃向双方中点上空，悬空长前摇，再朝目标俯冲，撞到目标或落地时中范围中伤害。 */
    static final class RyuseiOtoshi extends Attack {
        private static final int LEAP_TICKS = 14;
        private static final double APEX_HEIGHT = 7.0D;
        private static final double DIVE_SPEED = 1.8D;
        private static final int DIVE_MAX = 30;
        private Vec3 start;
        private Vec3 apex;
        private Vec3 diveDirection;
        private int hoverAt = -1;
        private int diveAt = -1;

        RyuseiOtoshi() {
            super(Move.RYUSEI_OTOSHI);
        }

        @Override
        boolean tick(TwilightBladeWarden warden) {
            if (windup(warden, 0, MEDIUM, true)) {
                start = warden.position();
                Vec3 target = warden.targetPosition();
                Vec3 middle = start.add(target).scale(0.5D);
                apex = new Vec3(middle.x, Math.max(start.y, target.y) + APEX_HEIGHT, middle.z);
                warden.setNoGravity(true);
                WardenCombat.leapStart(warden);
            }
            if (start != null && hoverAt < 0 && t > MEDIUM) {
                int leapTick = t - MEDIUM;
                double progress = Math.min(1.0D, leapTick / (double) LEAP_TICKS);
                double rise = Math.sin(progress * Math.PI / 2.0D);
                Vec3 desired = new Vec3(Mth.lerp(progress, start.x, apex.x), Mth.lerp(rise, start.y, apex.y),
                        Mth.lerp(progress, start.z, apex.z));
                warden.moveBy(desired.subtract(warden.position()));
                warden.faceTarget();
                WardenCombat.trail(warden, false);
                if (leapTick >= LEAP_TICKS) {
                    hoverAt = t + 1;
                }
            }
            if (hoverAt >= 0 && diveAt < 0) {
                warden.setDeltaMovement(Vec3.ZERO);
                if (windup(warden, hoverAt, BLUE_LONG, true)) {
                    Vec3 target = warden.targetPosition().add(0.0D, 0.5D, 0.0D);
                    Vec3 delta = target.subtract(warden.position());
                    diveDirection = delta.lengthSqr() > 1.0E-4D ? delta.normalize() : new Vec3(0.0D, -1.0D, 0.0D);
                    diveAt = t;
                    WardenCombat.dashStart(warden, false);
                }
            }
            if (diveAt >= 0 && t > diveAt) {
                warden.moveBy(diveDirection.scale(DIVE_SPEED));
                WardenCombat.trail(warden, false);
                boolean landed = warden.verticalCollision || warden.horizontalCollision || warden.onGround()
                        || warden.touchingTarget(0.5D) || t - diveAt >= DIVE_MAX;
                if (landed) {
                    warden.setNoGravity(false);
                    WardenCombat.strikeCircle(warden, warden.position(), MEDIUM_RADIUS, MEDIUM_DAMAGE, false);
                    return false;
                }
            }
            return true;
        }
    }

    // ===================== 红刀 =====================

    /** 终焉一文字：长前摇，前方 150° 大扇形加自身周围小圆，高伤害；随后同范围再一次中伤害。 */
    static final class ShuenIchimonji extends Attack {
        private static final int FOLLOW_UP = 8;

        ShuenIchimonji() {
            super(Move.SHUEN_ICHIMONJI);
        }

        @Override
        boolean tick(TwilightBladeWarden warden) {
            if (windup(warden, 0, RED_LONG, true)) {
                WardenCombat.strikeSectorAndCircle(warden, LARGE, 75.0D, SMALL, HIGH_DAMAGE, true);
            }
            if (t == RED_LONG + FOLLOW_UP) {
                WardenCombat.strikeSectorAndCircle(warden, LARGE, 75.0D, SMALL, MEDIUM_DAMAGE, true);
            }
            return t < RED_LONG + FOLLOW_UP + 6;
        }
    }

    /** 升龙闪：长前摇后追着目标冲刺；撞到目标造成中伤害并把它击飞，稍后在空中补一刀高伤害。 */
    static final class Shoryusen extends Attack {
        private static final double SPEED = 1.6D;
        private static final int RUSH_MAX = 18;
        private static final int LAUNCH_DELAY = 10;
        private final Set<UUID> hit = new HashSet<>();
        private int rushAt = -1;
        private int finishAt = -1;
        private LivingEntity victim;

        Shoryusen() {
            super(Move.SHORYUSEN);
        }

        @Override
        boolean tick(TwilightBladeWarden warden) {
            if (windup(warden, 0, RED_LONG, true)) {
                rushAt = t;
                WardenCombat.dashStart(warden, true);
            }
            if (rushAt >= 0 && finishAt < 0 && t > rushAt) {
                LivingEntity target = warden.getTarget();
                Vec3 aim = warden.targetPosition();
                Vec3 flat = new Vec3(aim.x - warden.getX(), 0.0D, aim.z - warden.getZ());
                boolean blocked = false;
                if (flat.lengthSqr() > 1.0E-4D) {
                    Vec3 direction = flat.normalize();
                    warden.faceDirection(direction);
                    blocked = warden.moveBy(direction.scale(Math.min(SPEED, flat.length())));
                }
                WardenCombat.trail(warden, true);
                for (LivingEntity entity : warden.contacts(0.6D)) {
                    if (hit.add(entity.getUUID()) && WardenCombat.hurt(warden, entity, MEDIUM_DAMAGE)
                            && entity == target && finishAt < 0) {
                        WardenCombat.launch(entity);
                        victim = entity;
                        finishAt = t + LAUNCH_DELAY;
                    }
                }
                if (finishAt < 0 && (blocked || t - rushAt >= RUSH_MAX)) {
                    return false;
                }
            }
            if (finishAt >= 0 && t >= finishAt) {
                if (victim != null && victim.isAlive() && warden.distanceToSqr(victim) < 16.0D * 16.0D) {
                    warden.faceTowards(victim.position());
                    WardenCombat.slashAt(warden, victim, true);
                    WardenCombat.hurt(warden, victim, HIGH_DAMAGE);
                }
                return false;
            }
            return true;
        }
    }

    /** 三重符阵：三段扩张的环形斩（半径 5 / 9 / 14），前两段中伤害，最后一段长前摇高伤害；每段都可招架。 */
    static final class SanjuFujin extends Attack {
        private static final double FIRST = 5.0D;

        SanjuFujin() {
            super(Move.SANJU_FUJIN);
        }

        @Override
        boolean tick(TwilightBladeWarden warden) {
            int second = RED_MEDIUM + 1;
            int third = second + RED_MEDIUM + 1;
            if (windup(warden, 0, RED_MEDIUM, true)) {
                WardenCombat.strikeCircle(warden, warden.position(), FIRST, MEDIUM_DAMAGE, true);
            }
            if (windup(warden, second, RED_MEDIUM, true)) {
                WardenCombat.strikeCircle(warden, warden.position(), LARGE, MEDIUM_DAMAGE, true);
            }
            if (windup(warden, third, RED_FINAL, true)) {
                WardenCombat.strikeCircle(warden, warden.position(), HUGE, HIGH_DAMAGE, true);
            }
            return t < third + RED_FINAL + 6;
        }
    }
}
