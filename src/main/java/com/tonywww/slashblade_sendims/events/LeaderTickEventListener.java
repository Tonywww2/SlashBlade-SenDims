package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.utils.MobAttackManager;
import com.tonywww.slashblade_sendims.utils.NBTUtils;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber
public class LeaderTickEventListener {

    public static final int MIN_SPECIAL_ATTACK_TICK = 60;
    public static final int MAX_SPECIAL_ATTACK_TICK = 200;
    public static final int PRE_PARRY_TICK = 30;
    public static final int PARRY_TICK = 15;

    public static final String SPECIAL_ATTACK_TICK_COUNT_PATH = "sbsd.sa.tickcount";
    public static final String SPECIAL_ATTACK_TARGET_TICK_COUNT_PATH = "sbsd.sa.targettickcount";

    public static final Vector3f PARRY_INDICATOR_FROM_COLOR = new Vector3f(0.3f, 0.1f, 0.1f);

    @SubscribeEvent
    public static void LivingTickEventListener(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        CompoundTag persistentData = living.getPersistentData();
        if (!persistentData.contains("apoth.boss") || !persistentData.getBoolean("apoth.boss")) return;

        if (living.level() instanceof ServerLevel serverLevel) {

            tickLeader(living, serverLevel, persistentData, living.tickCount);

        }


    }

    public static void tickLeader(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData, int currentTick) {
        if (entity instanceof Mob mob) {
            if (mob.getTarget() != null) {
                int saTargetTick = getSpecialAttackTargetTickCount(persistentData);

                if (saTargetTick <= 0) {
                    saTargetTick = serverLevel.random.nextInt(MIN_SPECIAL_ATTACK_TICK, MAX_SPECIAL_ATTACK_TICK);
                    setSpecialAttackTargetTickCount(persistentData, saTargetTick);
                }

                int saCurrentTick = getSpecialAttackTickCount(persistentData);

                int diff = saTargetTick - saCurrentTick;

                if (diff <= PRE_PARRY_TICK) {
                    if (diff <= PARRY_TICK) {
                        doLeaderParryIndicator(entity, serverLevel, diff);
                    } else {
                        doLeaderPreParryIndicator(entity, serverLevel, diff);

                    }
                }

                if (saCurrentTick >= saTargetTick) {
                    doLeaderSpecialAttack(entity, serverLevel);
                    saCurrentTick = 0;
                }

                saCurrentTick++;
                setSpecialAttackTickCount(persistentData, saCurrentTick);

            }

        }

    }

    private static void doLeaderSpecialAttack(LivingEntity entity, ServerLevel serverLevel) {
        MobAttackManager.doSlash(entity, -10.0F, 0x8B0000, Vec3.ZERO, true, false, 0.75f, KnockBacks.smash);
        MobAttackManager.doSlash(entity, 10.0F, 0x8B0000, Vec3.ZERO, true, false, 0.75f, KnockBacks.smash);

    }

    private static void doLeaderPreParryIndicator(LivingEntity entity, ServerLevel serverLevel, int tickBeforeAttack) {
        AABB boundBox = entity.getBoundingBox();
        double xPos = entity.getX();
        double yPos = boundBox.getYsize() + entity.getY() + 0.75d;
        double zPos = entity.getZ();

        spawnIndicatorParticles(serverLevel, ParticleTypes.SOUL_FIRE_FLAME, xPos, yPos, zPos, 1, 0.01d);
    }

    private static void doLeaderParryIndicator(LivingEntity entity, ServerLevel serverLevel, int tickBeforeAttack) {
        AABB boundBox = entity.getBoundingBox();
        double xPos = entity.getX();
        double yPos = boundBox.getYsize() + entity.getY() + 0.75d;
        double zPos = entity.getZ();

        float intensity = (float) tickBeforeAttack / PARRY_TICK;
        Vector3f toColor = new Vector3f(intensity, 0.1f * intensity, 0.1f * intensity);

        DustColorTransitionOptions dustOptions = new DustColorTransitionOptions(PARRY_INDICATOR_FROM_COLOR, toColor, 1.0f);

        spawnIndicatorParticles(serverLevel, dustOptions, xPos, yPos, zPos, 2, 10d);
    }

    private static void spawnIndicatorParticles(ServerLevel serverLevel, ParticleOptions particle, double xPos, double yPos, double zPos, int count, double speed) {
        serverLevel.sendParticles(particle,
                xPos, yPos, zPos,
                count,
                0d, 0.05d, 0d,
                speed);
        serverLevel.sendParticles(particle,
                xPos, yPos - 0.25d, zPos,
                count,
                0d, 0.05d, 0d,
                speed);
        serverLevel.sendParticles(particle,
                xPos, yPos - 0.5d, zPos,
                count,
                0d, 0.05d, 0d,
                speed);
        serverLevel.sendParticles(particle,
                xPos, yPos - 0.75d, zPos,
                count,
                0d, 0.05d, 0d,
                speed);
    }

    private static int getSpecialAttackTickCount(CompoundTag persistentData) {
        return NBTUtils.getSpecificField(persistentData, SPECIAL_ATTACK_TICK_COUNT_PATH);
    }

    private static void setSpecialAttackTickCount(CompoundTag persistentData, int val) {
        persistentData.putInt(SPECIAL_ATTACK_TICK_COUNT_PATH, val);
    }

    private static int getSpecialAttackTargetTickCount(CompoundTag persistentData) {
        return NBTUtils.getSpecificField(persistentData, SPECIAL_ATTACK_TARGET_TICK_COUNT_PATH);
    }

    private static void setSpecialAttackTargetTickCount(CompoundTag persistentData, int val) {
        persistentData.putInt(SPECIAL_ATTACK_TARGET_TICK_COUNT_PATH, val);
    }
}
