package com.tonywww.slashblade_sendims.entities.boss;

import com.tonywww.slashblade_sendims.entities.EntityVisualSlashEffect;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** 暮刃守卫的伤害结算与出招画面。所有伤害都是攻击力的倍率，走普通怪物攻击伤害（不穿透无敌帧）。 */
final class WardenCombat {
    static final int BLUE_COLOR = 0x3FA9FF;
    static final int RED_COLOR = 0xFF3B3B;
    private static final Vector3f BLUE_DUST = new Vector3f(0.25F, 0.66F, 1.0F);
    private static final Vector3f RED_DUST = new Vector3f(1.0F, 0.23F, 0.23F);

    private WardenCombat() {
    }

    // ---------- 伤害 ----------

    static boolean canHit(TwilightBladeWarden warden, LivingEntity entity) {
        if (entity == warden || !entity.isAlive() || entity.isSpectator() || entity instanceof ArmorStand) {
            return false;
        }
        if (entity instanceof Player player && player.isCreative()) {
            return false;
        }
        return !warden.isAlliedTo(entity);
    }

    static boolean hurt(TwilightBladeWarden warden, LivingEntity target, float multiplier) {
        float amount = (float) warden.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier;
        return target.hurt(warden.damageSources().mobAttack(warden), amount);
    }

    /** 对搜索范围内满足 inside 的目标各造成一次伤害，返回命中数。 */
    static int hitArea(TwilightBladeWarden warden, Vec3 center, double radius,
                       Predicate<LivingEntity> inside, float multiplier) {
        double vertical = Math.max(3.0D, radius * 0.5D);
        AABB box = new AABB(center, center).inflate(radius + 1.0D, vertical + 1.0D, radius + 1.0D);
        List<LivingEntity> victims = warden.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> canHit(warden, entity) && inside.test(entity));
        int count = 0;
        for (LivingEntity victim : victims) {
            if (hurt(warden, victim, multiplier)) {
                count++;
            }
        }
        return count;
    }

    /** 目标碰撞箱到中心点的水平距离（贴边即算命中）。 */
    static double horizontalDistance(Vec3 center, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        double dx = Math.max(Math.max(box.minX - center.x, 0.0D), center.x - box.maxX);
        double dz = Math.max(Math.max(box.minZ - center.z, 0.0D), center.z - box.maxZ);
        return Math.sqrt(dx * dx + dz * dz);
    }

    static boolean verticallyInside(Vec3 center, double radius, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        return box.maxY >= center.y - 2.0D && box.minY <= center.y + Math.max(3.0D, radius * 0.5D);
    }

    static boolean inCircle(Vec3 center, double radius, LivingEntity entity) {
        return horizontalDistance(center, entity) <= radius && verticallyInside(center, radius, entity);
    }

    static boolean inSector(Vec3 origin, float yaw, double radius, double halfAngle, LivingEntity entity) {
        if (!inCircle(origin, radius, entity)) {
            return false;
        }
        double dx = entity.getX() - origin.x;
        double dz = entity.getZ() - origin.z;
        if (dx * dx + dz * dz < 2.25D) {
            return true;
        }
        float toTarget = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        return Math.abs(Mth.wrapDegrees(toTarget - yaw)) <= halfAngle;
    }

    // ---------- 组合出招 ----------

    static void strikeCircle(TwilightBladeWarden warden, Vec3 center, double radius, float multiplier, boolean red) {
        hitArea(warden, center, radius, entity -> inCircle(center, radius, entity), multiplier);
        circleVisual(warden, center, radius, red);
    }

    static void strikeSector(TwilightBladeWarden warden, double radius, double halfAngle,
                             float multiplier, boolean red) {
        Vec3 origin = warden.position();
        float yaw = warden.getYRot();
        hitArea(warden, origin, radius, entity -> inSector(origin, yaw, radius, halfAngle, entity), multiplier);
        sectorVisual(warden, radius, halfAngle, red);
    }

    /** 前方扇形与自身周围小圆的合并范围，同一目标只吃一次伤害。 */
    static void strikeSectorAndCircle(TwilightBladeWarden warden, double sectorRadius, double halfAngle,
                                      double circleRadius, float multiplier, boolean red) {
        Vec3 origin = warden.position();
        float yaw = warden.getYRot();
        hitArea(warden, origin, Math.max(sectorRadius, circleRadius),
                entity -> inSector(origin, yaw, sectorRadius, halfAngle, entity)
                        || inCircle(origin, circleRadius, entity),
                multiplier);
        sectorVisual(warden, sectorRadius, halfAngle, red);
        circleVisual(warden, origin, circleRadius, red);
    }

    /** 冲刺途中碰到的目标各吃一次伤害。 */
    static void hitContacts(TwilightBladeWarden warden, Set<UUID> alreadyHit, float multiplier) {
        for (LivingEntity entity : warden.contacts(0.6D)) {
            if (alreadyHit.add(entity.getUUID())) {
                hurt(warden, entity, multiplier);
            }
        }
    }

    static void launch(LivingEntity target) {
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x * 0.2D, 1.15D, motion.z * 0.2D);
        target.hasImpulse = true;
        target.hurtMarked = true;
    }

    // ---------- 画面与音效 ----------

    static void circleVisual(TwilightBladeWarden warden, Vec3 center, double radius, boolean red) {
        int color = red ? RED_COLOR : BLUE_COLOR;
        float size = (float) Math.max(0.6D, radius / 4.0D);
        Vec3 slashPos = center.add(0.0D, warden.getBbHeight() * 0.5D, 0.0D);
        for (int i = 0; i < 3; i++) {
            EntityVisualSlashEffect.spawn(warden, slashPos, warden.getYRot() + i * 120.0F,
                    i * 12.0F - 12.0F, color, size, i > 0);
        }
        groundRing(warden, center, radius, red);
        warden.level().playSound(null, center.x, center.y, center.z, SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.5F, red ? 0.6F : 0.9F);
        if (radius >= 9.0D) {
            warden.level().playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 0.8F, 0.7F);
        }
    }

    static void sectorVisual(TwilightBladeWarden warden, double radius, double halfAngle, boolean red) {
        int color = red ? RED_COLOR : BLUE_COLOR;
        float size = (float) Math.max(0.6D, radius / 4.0D);
        Vec3 slashPos = warden.position().add(0.0D, warden.getBbHeight() * 0.5D, 0.0D)
                .add(warden.getLookAngle().scale(0.3D));
        EntityVisualSlashEffect.spawn(warden, slashPos, warden.getYRot() - 0.5F, 20.0F, color, size, false);
        if (halfAngle >= 60.0D) {
            EntityVisualSlashEffect.spawn(warden, slashPos, warden.getYRot() - 0.5F, 160.0F, color, size, true);
        }
        if (warden.level() instanceof ServerLevel level) {
            DustParticleOptions dust = new DustParticleOptions(red ? RED_DUST : BLUE_DUST, 1.6F);
            int points = Math.max(8, (int) (radius * halfAngle / 25.0D));
            for (int i = 0; i <= points; i++) {
                double angle = Math.toRadians(warden.getYRot() + 90.0F - halfAngle + 2.0D * halfAngle * i / points);
                double x = warden.getX() + Math.cos(angle) * radius;
                double z = warden.getZ() + Math.sin(angle) * radius;
                level.sendParticles(dust, x, warden.getY() + 0.2D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        warden.level().playSound(null, warden.getX(), warden.getY(), warden.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.HOSTILE, 1.5F, red ? 0.6F : 0.9F);
    }

    static void groundRing(TwilightBladeWarden warden, Vec3 center, double radius, boolean red) {
        if (!(warden.level() instanceof ServerLevel level)) {
            return;
        }
        DustParticleOptions dust = new DustParticleOptions(red ? RED_DUST : BLUE_DUST, 1.6F);
        int points = Math.max(16, (int) (Math.PI * 2.0D * radius / 0.7D));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(dust, x, center.y + 0.2D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, center.y + 0.5D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    static void slashAt(TwilightBladeWarden warden, LivingEntity target, boolean red) {
        int color = red ? RED_COLOR : BLUE_COLOR;
        Vec3 pos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        EntityVisualSlashEffect.spawn(warden, pos, warden.getYRot(), 75.0F, color, 1.2F, false);
        EntityVisualSlashEffect.spawn(warden, pos, warden.getYRot() + 90.0F, -75.0F, color, 1.2F, true);
        warden.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_CRIT,
                SoundSource.HOSTILE, 1.5F, 0.7F);
    }

    static void dashStart(TwilightBladeWarden warden, boolean red) {
        warden.level().playSound(null, warden.getX(), warden.getY(), warden.getZ(), SoundEvents.TRIDENT_RIPTIDE_1,
                SoundSource.HOSTILE, 1.2F, red ? 0.7F : 1.1F);
    }

    static void trail(TwilightBladeWarden warden, boolean red) {
        if (warden.level() instanceof ServerLevel level) {
            DustParticleOptions dust = new DustParticleOptions(red ? RED_DUST : BLUE_DUST, 1.4F);
            level.sendParticles(dust, warden.getX(), warden.getY() + warden.getBbHeight() * 0.5D, warden.getZ(),
                    4, 0.4D, 0.6D, 0.4D, 0.0D);
        }
    }

    static void leapStart(TwilightBladeWarden warden) {
        warden.level().playSound(null, warden.getX(), warden.getY(), warden.getZ(), SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.HOSTILE, 1.5F, 1.2F);
    }

    static void phaseBurst(TwilightBladeWarden warden) {
        if (warden.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, warden.getX(), warden.getY() + warden.getBbHeight() * 0.5D,
                    warden.getZ(), 60, 0.8D, 1.0D, 0.8D, 0.25D);
        }
        warden.level().playSound(null, warden.getX(), warden.getY(), warden.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.HOSTILE, 2.0F, 0.6F);
        warden.level().playSound(null, warden.getX(), warden.getY(), warden.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    static void invulnerableShimmer(TwilightBladeWarden warden) {
        if (warden.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, warden.getX(), warden.getY() + warden.getBbHeight() * 0.5D,
                    warden.getZ(), 12, 0.9D, 1.1D, 0.9D, 0.4D);
        }
    }
}
