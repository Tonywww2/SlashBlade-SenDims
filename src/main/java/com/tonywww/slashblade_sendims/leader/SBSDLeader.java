package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.utils.MobAttackManager;
import com.tonywww.slashblade_sendims.utils.NBTUtils;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class SBSDLeader {

    public static final int MIN_SPECIAL_ATTACK_TICK = 60;
    public static final int MAX_SPECIAL_ATTACK_TICK = 200;
    public static final int PRE_PARRY_TICK = 30;
    public static final int PARRY_TICK = 15;
    public static final int END_PARRIED_TICK = 60;

    public static final String APOTH_BOSS = "apoth.boss";
    public static final String LEADER_ACTION_TICK_COUNT_PATH = "sbsd.act.tick";
    public static final String LEADER_NEXT_ACTION_TICK_COUNT_PATH = "sbsd.act.next";
    public static final String IS_PARRIABLE_PATH = "sbsd.isparriable";
    public static final String IS_PARRIED_PATH = "sbsd.isparried";

    public static final float NOT_PARRIED_DAMAGE_AMOUNT = 0.25f;

    public static Set<ResourceLocation> PARRY_COMBOS = new HashSet<>();

    static {
        PARRY_COMBOS.add(ComboStateRegistry.RAPID_SLASH.getId());
        PARRY_COMBOS.add(ComboStateRegistry.UPPERSLASH.getId());
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

    public static void handleParryActions(SlashBladeEvent.HitEvent event, LivingEntity target, CompoundTag persistentData) {
        ISlashBladeState bladeState = event.getSlashBladeState();
        ResourceLocation currentCS = bladeState.getComboSeq();

        if (getParriable(persistentData) && PARRY_COMBOS.contains(currentCS)) {
            setLeaderParried(target, persistentData);
        }
    }

    public static void scaleIncomingDamage(LivingHurtEvent event, CompoundTag persistentData) {
        if (!SBSDLeader.getParried(persistentData)) {
            float damage = event.getAmount();
            damage *= SBSDLeader.NOT_PARRIED_DAMAGE_AMOUNT;
            event.setAmount(damage);
        }
    }

    public static void setLeaderParried(LivingEntity target, CompoundTag persistentData) {
        setParried(persistentData, true);
        setParriable(persistentData, false);
        setLeaderActionTickCount(persistentData, 0);
        setLeaderNextActionTickCount(persistentData, 0);

        StunManager.setStun(target, END_PARRIED_TICK);

    }

    public static void tickParried(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData) {
        doLeaderParriedIndicator(entity, serverLevel);
        int endParriedTick = getLeaderNextActionTickCount(persistentData);
        if (endParriedTick <= 0) {
            endParriedTick = END_PARRIED_TICK;
            setLeaderNextActionTickCount(persistentData, endParriedTick);
        }

        int currentParriedTick = getLeaderActionTickCount(persistentData);

        if (currentParriedTick > endParriedTick) {
            setParried(persistentData, false);
            currentParriedTick = 0;
            setLeaderNextActionTickCount(persistentData, 0);

        } else {
            currentParriedTick++;

        }

        setLeaderActionTickCount(persistentData, currentParriedTick);

    }

    public static void tickNormal(LivingEntity entity, ServerLevel serverLevel, CompoundTag persistentData) {
        int saTargetTick = getLeaderNextActionTickCount(persistentData);

        if (saTargetTick <= 0) {
            saTargetTick = serverLevel.random.nextInt(MIN_SPECIAL_ATTACK_TICK, MAX_SPECIAL_ATTACK_TICK);
            setLeaderNextActionTickCount(persistentData, saTargetTick);
        }

        int saCurrentTick = getLeaderActionTickCount(persistentData);
        if (saCurrentTick > saTargetTick) {
            doLeaderSpecialAttack(entity, serverLevel);
            saCurrentTick = 0;
            setLeaderNextActionTickCount(persistentData, 0);
            setParriable(persistentData, false);

        } else {
            saCurrentTick++;

        }

        int diff = saTargetTick - saCurrentTick;
        if (diff <= PRE_PARRY_TICK) {
            if (diff <= PARRY_TICK) {
                doLeaderParryIndicator(entity, serverLevel, diff);
                if (diff >= 0) {
                    setParriable(persistentData, true);
                }
            } else {
                doLeaderPreParryIndicator(entity, serverLevel, diff);
                setParriable(persistentData, false);

            }
        } else {
            setParriable(persistentData, false);

        }

        setLeaderActionTickCount(persistentData, saCurrentTick);

    }

    public static void doLeaderSpecialAttack(LivingEntity entity, ServerLevel serverLevel) {
        MobAttackManager.doSlash(entity, -30.0F, 0x8B0000, Vec3.ZERO, true, false, 0.3f, KnockBacks.smash);
        SenDims.serverScheduler.schedule(3, () -> {
            MobAttackManager.doSlash(entity, 15.0F, 0x8B0000, Vec3.ZERO, true, false, 0.6f, KnockBacks.smash);
        });
        SenDims.serverScheduler.schedule(5, () -> {
            MobAttackManager.doSlash(entity, -15.0F, 0x8B0000, Vec3.ZERO, true, false, 0.6f, KnockBacks.smash);
        });

    }

    public static void doLeaderParriedIndicator(LivingEntity entity, ServerLevel serverLevel) {
        AABB boundBox = entity.getBoundingBox();
        double xPos = entity.getX();
        double yPos = boundBox.getYsize() + entity.getY() + 0.75d;
        double zPos = entity.getZ();

        spawnIndicatorParticles(serverLevel, ParticleTypes.ANGRY_VILLAGER, xPos, yPos, zPos, 1, 0.01d);
    }

    public static void doLeaderPreParryIndicator(LivingEntity entity, ServerLevel serverLevel, int tickBeforeAttack) {
        AABB boundBox = entity.getBoundingBox();
        double xPos = entity.getX();
        double yPos = boundBox.getYsize() + entity.getY() + 0.75d;
        double zPos = entity.getZ();

        spawnIndicatorParticles(serverLevel, ParticleTypes.SOUL_FIRE_FLAME, xPos, yPos, zPos, 1, 0.01d);
    }

    public static void doLeaderParryIndicator(LivingEntity entity, ServerLevel serverLevel, int tickBeforeAttack) {
        AABB boundBox = entity.getBoundingBox();
        double xPos = entity.getX();
        double yPos = boundBox.getYsize() + entity.getY() + 0.75d;
        double zPos = entity.getZ();

        float intensity = (float) tickBeforeAttack / PARRY_TICK;
        Vector3f toColor = new Vector3f(intensity, 0.1f * intensity, 0.1f * intensity);

        DustColorTransitionOptions dustOptions = new DustColorTransitionOptions(PARRY_INDICATOR_FROM_COLOR, toColor, 1.0f);

        spawnIndicatorParticles(serverLevel, dustOptions, xPos, yPos, zPos, 2, 10d);
    }

    public static void spawnIndicatorParticles(ServerLevel serverLevel, ParticleOptions particle, double xPos, double yPos, double zPos, int count, double speed) {
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

    public static boolean getParried(CompoundTag persistentData) {
        return NBTUtils.getSpecificBoolField(persistentData, IS_PARRIED_PATH);
    }

    public static void setParried(CompoundTag persistentData, boolean b) {
        persistentData.putBoolean(IS_PARRIED_PATH, b);
    }

    public static boolean getParriable(CompoundTag persistentData) {
        return NBTUtils.getSpecificBoolField(persistentData, IS_PARRIABLE_PATH);
    }

    public static void setParriable(CompoundTag persistentData, boolean b) {
        persistentData.putBoolean(IS_PARRIABLE_PATH, b);
    }

    public static int getLeaderActionTickCount(CompoundTag persistentData) {
        return NBTUtils.getSpecificIntField(persistentData, LEADER_ACTION_TICK_COUNT_PATH);
    }

    public static void setLeaderActionTickCount(CompoundTag persistentData, int val) {
        persistentData.putInt(LEADER_ACTION_TICK_COUNT_PATH, val);
    }

    public static int getLeaderNextActionTickCount(CompoundTag persistentData) {
        return NBTUtils.getSpecificIntField(persistentData, LEADER_NEXT_ACTION_TICK_COUNT_PATH);
    }

    public static void setLeaderNextActionTickCount(CompoundTag persistentData, int val) {
        persistentData.putInt(LEADER_NEXT_ACTION_TICK_COUNT_PATH, val);
    }
}
