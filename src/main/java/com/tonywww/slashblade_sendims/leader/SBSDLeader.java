package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import com.tonywww.slashblade_sendims.utils.MobAttackManager;
import com.tonywww.slashblade_sendims.entities.EntityMobDrive;
import com.tonywww.slashblade_sendims.utils.NBTUtils;
import com.tonywww.slashblade_sendims.SBSDValues;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.joml.Vector3f;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import twilightforest.entity.boss.Naga;

import java.util.List;

public class SBSDLeader {

    @FunctionalInterface
    public interface SAFunction {
        void apply(LivingEntity entity, ServerLevel serverLevel);
    }

    public static void tickLeader(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData, int currentTick) {
        if (LeaderApi.isParried(entity)) {
            tickParried(entity, serverLevel, persistentData);
            return;
        }
        if (entity instanceof Mob mob) {
            if (mob.getTarget() != null) {
                tickNormal(entity, serverLevel, persistentData);
            }
        }
    }

    @Deprecated(forRemoval = false)
    public static void tickBossLeader(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData, int currentTick) {
        if (entity instanceof Naga naga) {
            NagaLeaderController.tick(naga, serverLevel);
        }
    }

    @Deprecated(forRemoval = false)
    public static boolean handleParryActions(SlashBladeEvent.HitEvent event, LivingEntity target, CompoundTag persistentData) {
        return LeaderCombatHandler.handleSlashBladeParry(event, target);
    }

    @Deprecated(forRemoval = false)
    public static void scaleIncomingDamage(LivingHurtEvent event, CompoundTag persistentData) {
        LeaderCombatHandler.scaleIncomingDamage(event);
    }

    @Deprecated(forRemoval = false)
    public static void setLeaderParried(LivingEntity target, CompoundTag persistentData) {
        setParried(persistentData, true);
        setParriable(persistentData, false);
        setLeaderActionTickCount(persistentData, 0);
        setLeaderNextActionTickCount(persistentData, 0);

    }

    public static void tickBossParried(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData) {
        if (tickParried(entity, serverLevel, persistentData)) {
            if (entity instanceof Naga naga) {
                naga.getMovementAI().doDaze();
            }
        } else {

        }
    }

    public static boolean tickParried(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData) {
        doLeaderParriedIndicator(entity, serverLevel);
        if (LeaderStateStorage.hasParriedDeadline(entity)) {
            if (LeaderStateStorage.hasParriedDeadlineExpired(entity)) {
                LeaderManager.recover(entity);
                return false;
            }
            return true;
        }

        boolean stillParried = true;
        int endParriedTick = getLeaderNextActionTickCount(persistentData);
        if (endParriedTick <= 0) {
            endParriedTick = SBSDValues.END_PARRIED_TICK;
            setLeaderNextActionTickCount(persistentData, endParriedTick);
        }

        int currentParriedTick = getLeaderActionTickCount(persistentData);

        if (LeaderTiming.isParriedFinished(currentParriedTick, endParriedTick)) {
            LeaderManager.recover(entity);
            currentParriedTick = 0;
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
            LeaderManager.setManagedParryable(entity, false);

        } else {
            saCurrentTick++;

        }
        setLeaderActionTickCount(persistentData, saCurrentTick);

        int diff = saTargetTick - saCurrentTick;
        if (diff <= SBSDValues.PRE_N_ATTACK_TICK) {
            if (diff <= SBSDValues.PRE_PARRY_TICK) {
                if (diff == SBSDValues.PRE_PARRY_TICK) {
                    doLeaderAttack(entity, serverLevel);
                }
                if (diff <= SBSDValues.PARRY_TICK) {
                    doLeaderParryIndicator(entity, serverLevel, diff);
                    if (LeaderTiming.isManagedParryWindowOpen(diff)) {
                        LeaderManager.setManagedParryable(entity, true);
                    }
                } else {
                    LeaderManager.setManagedParryable(entity, false);

                }
            } else {
                doLeaderPreAttackIndicator(entity, serverLevel, diff);
                LeaderManager.setManagedParryable(entity, false);

            }

        } else {
            LeaderManager.setManagedParryable(entity, false);

        }

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

        List<Player> entities = serverLevel.getEntitiesOfClass(Player.class, entity.getBoundingBox().inflate(6.0D));
        for (Player player : entities) {
            if (player.isAlive() && !(player instanceof FakePlayer)) {
                player.addEffect(new MobEffectInstance(ALObjects.MobEffects.GRIEVOUS.get(), 200, 1));
            }
        }
    }

    // 首领SA
    // Boss
    public static void doLeaderSAMagicSLash(LivingEntity entity, ServerLevel serverLevel) {
        MobAttackManager.doSlash(entity, 90, 3d, 0.75f, 0xb16cc4, Vec3.ZERO,
                true, false, true, 0.2f, KnockBacks.cancel);
        MobAttackManager.doSlash(entity, 210, 3d, 0.75f, 0xb16cc4, Vec3.ZERO,
                true, false, true, 0.2f, KnockBacks.cancel);
        MobAttackManager.doSlash(entity, -30, 3d, 0.75f, 0xb16cc4, Vec3.ZERO,
                true, false, true, 0.2f, KnockBacks.cancel);

    }

    public static void doLeaderSATripleDrive(LivingEntity entity, ServerLevel serverLevel) {
        EntityMobDrive.doSlash(entity, 0f, 0f, 60, 0x126000, Vec3.ZERO,
                false, 0.3f, KnockBacks.cancel, 1.5f, 2f);
        EntityMobDrive.doSlash(entity, 90f, 0f, 60, 0x126000, Vec3.ZERO,
                false, 0.3f, KnockBacks.cancel, 1.5f, 2f);

    }

    public static void doLeaderSAMagicDrive(LivingEntity entity, ServerLevel serverLevel) {
        EntityMobDrive.doSlash(entity, 60, 0f, 60, 0xb16cc4, Vec3.ZERO,
                false, 0.15f, KnockBacks.cancel, 0.75f, 0.75f);
        EntityMobDrive.doSlash(entity, 180, 0f, 60, 0xb16cc4, Vec3.ZERO,
                false, 0.15f, KnockBacks.cancel, 0.75f, 0.75f);
        EntityMobDrive.doSlash(entity, 300, 0f, 60, 0xb16cc4, Vec3.ZERO,
                false, 0.15f, KnockBacks.cancel, 0.75f, 0.75f);

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

        Vector3f color = LeaderIndicatorVisuals.dangerColor(tickBeforeAttack + 2);
        DustColorTransitionOptions dustOptions = new DustColorTransitionOptions(
            color, LeaderIndicatorVisuals.DANGER_END_COLOR, 1.0f);

        spawnIndicatorParticles(serverLevel, dustOptions, xPos, yPos, zPos, 2, 10d);
    }

    public static void spawnIndicatorParticles(ServerLevel serverLevel, ParticleOptions particle, double xPos, double yPos, double zPos, int count, double speed) {
        int points = 16;
        double radius = 2.0d;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double px = xPos + radius * Math.cos(angle);
            double pz = zPos + radius * Math.sin(angle);
            serverLevel.sendParticles(particle, px, yPos, pz, count, 0d, 0d, 0d, speed);
        }
    }

    @Deprecated(forRemoval = false)
    public static boolean getParried(CompoundTag persistentData) {
        return NBTUtils.getSpecificBoolField(persistentData, SBSDValues.IS_PARRIED_PATH);
    }

    @Deprecated(forRemoval = false)
    public static void setParried(CompoundTag persistentData, boolean b) {
        persistentData.putBoolean(SBSDValues.IS_PARRIED_PATH, b);
    }

    @Deprecated(forRemoval = false)
    public static boolean getParriable(CompoundTag persistentData) {
        return NBTUtils.getSpecificBoolField(persistentData, SBSDValues.IS_PARRIABLE_PATH);
    }

    @Deprecated(forRemoval = false)
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

            double scale = SBSDValues.LEADER_HP_SCALE;
            ResourceLocation rl = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
            // TODO 等修
            if (rl != null && "terra_entity".equals(rl.getNamespace())) {
                scale = SBSDValues.LEADER_HP_SCALE_RT;
            }
            instance.addPermanentModifier(new AttributeModifier("sbsd.leader.health", scale, AttributeModifier.Operation.MULTIPLY_TOTAL));
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
