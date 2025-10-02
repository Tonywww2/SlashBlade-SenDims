package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import com.tonywww.slashblade_sendims.utils.CuriosUtils;
import com.tonywww.slashblade_sendims.utils.MobAttackManager;
import com.tonywww.slashblade_sendims.entities.EntityMobDrive;
import com.tonywww.slashblade_sendims.utils.NBTUtils;
import com.tonywww.slashblade_sendims.SBSDValues;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.joml.Vector3f;
import twilightforest.entity.ai.goal.NagaMovementPattern;
import twilightforest.entity.boss.Naga;

public class SBSDLeader {

    @FunctionalInterface
    public interface SAFunction {
        void apply(LivingEntity entity, ServerLevel serverLevel);
    }

    public static final Vector3f PARRY_INDICATOR_FROM_COLOR = new Vector3f(0.3f, 0.1f, 0.1f);

    public static void tickLeader(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData, int currentTick) {
        if (entity instanceof Mob mob) {
            if (mob.getTarget() != null) {
                boolean isParried = getParried(persistentData);
                if (isParried) {
                    // 被击破
                    tickParried(entity, serverLevel, persistentData);

                } else {
                    // 通常状态
                    tickNormal(entity, serverLevel, persistentData);

                }
            }

        }

    }

    public static void tickBossLeader(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData, int currentTick) {
        if (entity instanceof Mob mob) {
            if (mob.getTarget() != null) {
                boolean isParried = getParried(persistentData);
                if (isParried) {
                    // 被击破
                    tickBossParried(entity, serverLevel, persistentData);

                } else {
                    // 通常状态
//                    tickNormal(entity, serverLevel, persistentData);
                    if (entity instanceof Naga naga) {
                        if (naga.getMovementAI().getState() == NagaMovementPattern.MovementState.INTIMIDATE) {
                            SBSDLeader.doLeaderParryIndicator(naga, (ServerLevel) naga.level(), 10);
                            SBSDLeader.setParriable(persistentData, true);
                        } else {
                            SBSDLeader.setParriable(persistentData, false);
                        }
                    }


                }
            }

        }

    }

    public static boolean handleParryActions(SlashBladeEvent.HitEvent event, LivingEntity target, CompoundTag persistentData) {
        ISlashBladeState bladeState = event.getSlashBladeState();
        ResourceLocation currentCS = bladeState.getComboSeq();

        if (getParriable(persistentData) && SBSDValues.PARRY_COMBOS.contains(currentCS)) {
            setLeaderParried(target, persistentData);
            // TODO 招架追加攻击做成饰品
            LivingEntity user = event.getUser();
            AttackManager.doSlash(user, 45.0F, 0x6cf243, Vec3.ZERO, false, false, 1.25f, KnockBacks.meteor);
            float amount = (float) SBSDAttributes.getAttributeValue(event.getUser(), CuriosUtils.SPRINT_CD.get());
            user.heal(amount);
            return true;
        }
        return false;
    }

    public static void scaleIncomingDamage(LivingHurtEvent event, CompoundTag persistentData) {
        if (SBSDLeader.getParried(persistentData)) {
            float damage = event.getAmount();
            damage *= SBSDValues.PARRIED_DAMAGE_SCALE;
            event.setAmount(damage);
        }
    }

    public static void setLeaderParried(LivingEntity target, CompoundTag persistentData) {
        setParried(persistentData, true);
        setParriable(persistentData, false);
        setLeaderActionTickCount(persistentData, 0);
        setLeaderNextActionTickCount(persistentData, 0);

    }

    public static void tickBossParried(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData) {
        if(tickParried(entity, serverLevel, persistentData)) {
            if (entity instanceof Naga naga) {
                naga.getMovementAI().doDaze();
            }
        } else {

        }
    }

    public static boolean tickParried(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData) {
        boolean stillParried = true;
        doLeaderParriedIndicator(entity, serverLevel);
        int endParriedTick = getLeaderNextActionTickCount(persistentData);
        if (endParriedTick <= 0) {
            endParriedTick = SBSDValues.END_PARRIED_TICK;
            setLeaderNextActionTickCount(persistentData, endParriedTick);
        }

        int currentParriedTick = getLeaderActionTickCount(persistentData);

        if (currentParriedTick > endParriedTick) {
            setParried(persistentData, false);
            currentParriedTick = 0;
            setLeaderNextActionTickCount(persistentData, 0);
            stillParried = false;

        } else {
            currentParriedTick++;

        }

        setLeaderActionTickCount(persistentData, currentParriedTick);
        return stillParried;
    }

    public static void tickNormal(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData) {
        int saTargetTick = getLeaderNextActionTickCount(persistentData);

        if (saTargetTick <= 0) {
            saTargetTick = serverLevel.random.nextInt(SBSDValues.MIN_SPECIAL_ATTACK_TICK, SBSDValues.MAX_SPECIAL_ATTACK_TICK);
            setLeaderNextActionTickCount(persistentData, saTargetTick);
        }

        int saCurrentTick = getLeaderActionTickCount(persistentData);
        if (saCurrentTick > saTargetTick) {
//            doLeaderSATripleSlash(entity, serverLevel);
            doLeaderSA(entity, serverLevel);
            saCurrentTick = 0;
            setLeaderNextActionTickCount(persistentData, 0);
            setParriable(persistentData, false);

        } else {
            saCurrentTick++;

        }

        int diff = saTargetTick - saCurrentTick;
        if (diff <= SBSDValues.PRE_N_ATTACK_TICK) {
            if (diff <= SBSDValues.PRE_PARRY_TICK) {
                if (diff == SBSDValues.PRE_PARRY_TICK) {
                    doLeaderAttack(entity, serverLevel);
                }
                if (diff <= SBSDValues.PARRY_TICK) {
                    doLeaderParryIndicator(entity, serverLevel, diff);
                    if (diff >= 0) {
                        setParriable(persistentData, true);
                    }
                } else {
                    setParriable(persistentData, false);

                }
            } else {
                doLeaderPreAttackIndicator(entity, serverLevel, diff);
                setParriable(persistentData, false);

            }

        } else {
            setParriable(persistentData, false);

        }

        setLeaderActionTickCount(persistentData, saCurrentTick);

    }

    public static void doLeaderAttack(LivingEntity entity, ServerLevel serverLevel) {
        if (entity.getRandom().nextBoolean()) {
            MobAttackManager.doSlash(entity, 45.0F, 7d, 0.75f, 0xffd2d2, Vec3.ZERO,
                    true, false, false, 0.45f, KnockBacks.cancel);
            SenDims.serverScheduler.schedule(5, () -> {
                MobAttackManager.doSlash(entity, 55.0F, 7d, 0.75f, 0xffd2d2, Vec3.ZERO,
                        true, false, true, 0.45f, KnockBacks.smash);
            });
        } else {
            MobAttackManager.doSlash(entity, -45.0F, 7d, 0.75f, 0xffd2d2, Vec3.ZERO,
                    true, false, false, 0.45f, KnockBacks.cancel);
            SenDims.serverScheduler.schedule(5, () -> {
                MobAttackManager.doSlash(entity, -55.0F, 7d, 0.75f, 0xffd2d2, Vec3.ZERO,
                        true, false, true, 0.45f, KnockBacks.smash);
            });
        }

    }

    public static void doLeaderSA(LivingEntity entity, ServerLevel serverLevel) {
        SBSDValues.ALL_LEADER_SA.get(serverLevel.getRandom().nextInt(SBSDValues.ALL_LEADER_SA.size())).apply(entity, serverLevel);
    }

    // 首领SA
    public static void doLeaderSAQuickSLash(LivingEntity entity, ServerLevel serverLevel) {
        MobAttackManager.doSlash(entity, 2.0F, 13d, 2f, 0xff9b9b, Vec3.ZERO,
                true, false, false, 1.0f, KnockBacks.toss);

    }

    public static void doLeaderSATripleSlash(LivingEntity entity, ServerLevel serverLevel) {
        MobAttackManager.doSlash(entity, -30.0F, 7.5d, 0.75f, 0x8B0000, Vec3.ZERO,
                true, false, true, 0.3f, KnockBacks.meteor);
        SenDims.serverScheduler.schedule(5, () -> {
            MobAttackManager.doSlash(entity, 15.0F, 9d, 1.5f, 0x6d0000, Vec3.ZERO,
                    true, false, true, 0.5f, KnockBacks.cancel);
        });
        SenDims.serverScheduler.schedule(7, () -> {
            MobAttackManager.doSlash(entity, -15.0F, 9d, 1.5f, 0x6d0000, Vec3.ZERO,
                    true, false, true, 1.1f, KnockBacks.smash);
        });

    }

    public static void doLeaderSATripleDrive(LivingEntity entity, ServerLevel serverLevel) {
        EntityMobDrive.doSlash(entity, 0f, 0f, 60, 0x126000, Vec3.ZERO,
                false, 0.3f, KnockBacks.cancel, 1.5f, 2f);
        EntityMobDrive.doSlash(entity, 90f, 0f, 60, 0x126000, Vec3.ZERO,
                false, 0.3f, KnockBacks.cancel, 1.5f, 2f);

    }

    public static void doLeaderParriedIndicator(LivingEntity entity, ServerLevel serverLevel) {
        AABB boundBox = entity.getBoundingBox();
        double xPos = entity.getX();
        double yPos = boundBox.getYsize() + entity.getY() + 0.75d;
        double zPos = entity.getZ();

        spawnIndicatorParticles(serverLevel, ParticleTypes.ANGRY_VILLAGER, xPos, yPos, zPos, 1, 0.01d);
    }

    public static void doLeaderPreAttackIndicator(LivingEntity entity, ServerLevel serverLevel, int tickBeforeAttack) {
        AABB boundBox = entity.getBoundingBox();
        double xPos = entity.getX();
        double yPos = boundBox.getYsize() / 2 + entity.getY();
        double zPos = entity.getZ();

        spawnIndicatorParticles(serverLevel, ParticleTypes.SOUL_FIRE_FLAME, xPos, yPos, zPos, 1, 0.01d);
    }

    public static void doLeaderParryIndicator(LivingEntity entity, ServerLevel serverLevel, int tickBeforeAttack) {
        AABB boundBox = entity.getBoundingBox();
        double xPos = entity.getX();
        double yPos = boundBox.getYsize() / 2 + entity.getY();
        double zPos = entity.getZ();

        float intensity = (float) tickBeforeAttack / SBSDValues.PARRY_TICK;
        Vector3f toColor = new Vector3f(0.5f * intensity, 0.05f * intensity, 0.05f * intensity);

        DustColorTransitionOptions dustOptions = new DustColorTransitionOptions(PARRY_INDICATOR_FROM_COLOR, toColor, 1.0f);

        spawnIndicatorParticles(serverLevel, dustOptions, xPos, yPos, zPos, 2, 10d);
    }

    public static void spawnIndicatorParticles(ServerLevel serverLevel, ParticleOptions particle, double xPos, double yPos, double zPos, int count, double speed) {
        int points = 16;
        double radius = 1;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double px = xPos + radius * Math.cos(angle);
            double pz = zPos + radius * Math.sin(angle);
            serverLevel.sendParticles(particle, px, yPos, pz, count, 0d, 0d, 0d, speed);
        }
    }

    public static boolean getParried(CompoundTag persistentData) {
        return NBTUtils.getSpecificBoolField(persistentData, SBSDValues.IS_PARRIED_PATH);
    }

    public static void setParried(CompoundTag persistentData, boolean b) {
        persistentData.putBoolean(SBSDValues.IS_PARRIED_PATH, b);
    }

    public static boolean getParriable(CompoundTag persistentData) {
        return NBTUtils.getSpecificBoolField(persistentData, SBSDValues.IS_PARRIABLE_PATH);
    }

    public static void setParriable(CompoundTag persistentData, boolean b) {
        persistentData.putBoolean(SBSDValues.IS_PARRIABLE_PATH, b);
    }

    public static int getLeaderActionTickCount(CompoundTag persistentData) {
        return NBTUtils.getSpecificIntField(persistentData, SBSDValues.LEADER_ACTION_TICK_COUNT_PATH);
    }

    public static void setLeaderActionTickCount(CompoundTag persistentData, int val) {
        persistentData.putInt(SBSDValues.LEADER_ACTION_TICK_COUNT_PATH, val);
    }

    public static int getLeaderNextActionTickCount(CompoundTag persistentData) {
        return NBTUtils.getSpecificIntField(persistentData, SBSDValues.LEADER_NEXT_ACTION_TICK_COUNT_PATH);
    }

    public static void setLeaderNextActionTickCount(CompoundTag persistentData, int val) {
        persistentData.putInt(SBSDValues.LEADER_NEXT_ACTION_TICK_COUNT_PATH, val);
    }

    public static void initializeLeader(LivingEntity living, CompoundTag persistentData) {
        AttributeInstance instance = living.getAttribute(Attributes.MAX_HEALTH);
        if (instance != null && !persistentData.contains(SBSDValues.IS_INITIALIZED)) {
            instance.addPermanentModifier(new AttributeModifier("sbsd.leader.health", SBSDValues.LEADER_HP_SCALE, AttributeModifier.Operation.MULTIPLY_TOTAL));
            persistentData.putBoolean(SBSDValues.IS_INITIALIZED, true);
        }
        living.setHealth(living.getMaxHealth());

        if (living.level() instanceof ServerLevel serverLevel) {
            ServerScoreboard scoreboard = serverLevel.getScoreboard();
            PlayerTeam team = scoreboard.getPlayerTeam(SBSDValues.LEADER_TEAM_NAME);
            if (team == null) {
                team = scoreboard.addPlayerTeam(SBSDValues.LEADER_TEAM_NAME);
                team.setAllowFriendlyFire(false);
            }
            scoreboard.addPlayerToTeam(living.getScoreboardName(), team);

        }

    }
}
