package com.tonywww.slashblade_sendims;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import com.tonywww.slashblade_sendims.utils.MobAttackManager;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import se.mickelus.tetra.effect.ItemEffect;
import twilightforest.init.TFEntities;

import java.util.*;

public class SBSDValues {
    public static final int CANCELED_CD = 8;
    // SPRINT
    public static final int SPRINT_CD = 50;
    public static final String SPRINT_CD_PATH = "sbsd.sprint.cd";
    public static final String SPRINT_SUCCESSED_PATH = "sbsd.sprint.cd_success";
    /** 闪避的行动力消耗 */
    public static final int SPRINT_COST = 100;
    public static final double SPRINT_COST_PERC = 0.05;
    /** 闪避成功恢复的无敌时间 */
    public static final int UNTOUCHABLE_TICK = 10;
    /** 闪避成功恢复的行动力 */
    public static final int SPRINT_SUCCESS_AP = 200;

    // HIT
    public static final double HIT_LEADER_AP = 64d;
    public static final Map<ResourceLocation, Integer> COMBO_COST_MAP = new HashMap<>();

    static {
        COMBO_COST_MAP.put(ComboStateRegistry.RAPID_SLASH.getId(), -800);
        COMBO_COST_MAP.put(ComboStateRegistry.UPPERSLASH.getId(), -400);
    }

    // Tetra
    public static final ItemEffect MANA_RESONANCE = ItemEffect.get("mana_resonance");

    // LEADER
    /** 首领（Leader）触发特殊攻击（Special Attack）的最小随机 Tick 间隔 */
    public static final int MIN_SPECIAL_ATTACK_TICK = 100;
    /** 首领（Leader）触发特殊攻击（Special Attack）的最大随机 Tick 间隔 */
    public static final int MAX_SPECIAL_ATTACK_TICK = 200;
    /** 攻击前摇总阶段的 Tick 临界值（距离攻击仅剩 n tick 时开始进入预警流程） */
    public static final int PRE_N_ATTACK_TICK = 60;
    /** 准备招架阶段的 Tick 临界值（距离攻击仅剩 n tick 时触发普通前摇，并接近招架窗口） */
    public static final int PRE_PARRY_TICK = 40;
    /** 可被招架的窗口 Tick 长度（距离攻击仅剩 n tick 内时） */
    public static final int PARRY_TICK = 20;
    /** 首领被击破（Parried）后处于瘫痪状态的总 Tick 时间 */
    public static final int END_PARRIED_TICK = 180;
    /** 普通首领的初始最大生命值加成倍率 */
    public static final double LEADER_HP_SCALE = 5.0d;
    /** 针对特定实体首领（Terra）的生命值加成倍率*/
    public static final double LEADER_HP_SCALE_RT = Math.floor(Math.sqrt(LEADER_HP_SCALE)) - 0.5d;
    /** 首领处于招架击破（Parried）状态时，所承受的伤害放大倍率 */
    public static final float PARRIED_DAMAGE_SCALE = 5.0f;

    public static final String BOSS_LEADER = "sbsd.boss";
    public static final String APOTH_BOSS = "apoth.boss";
    public static final String LEADER_ACTION_TICK_COUNT_PATH = "sbsd.act.tick";
    public static final String LEADER_NEXT_ACTION_TICK_COUNT_PATH = "sbsd.act.next";
    public static final String IS_PARRIABLE_PATH = "sbsd.isparriable";
    public static final String IS_PARRIED_PATH = "sbsd.isparried";
    public static final String IS_INITIALIZED = "sbsd.init";
    public static final String LEADER_TEAM_NAME = "sbsd_leaders";

    public static Set<ResourceLocation> PARRY_COMBOS = new HashSet<>();

    static {
        PARRY_COMBOS.add(ComboStateRegistry.RAPID_SLASH.getId());
        PARRY_COMBOS.add(ComboStateRegistry.UPPERSLASH.getId());
    }

    public static final List<SBSDLeader.SAFunction> ALL_LEADER_SA = new ArrayList<>(10);

    static {
        ALL_LEADER_SA.add((LivingEntity entity, ServerLevel serverLevel) -> {
            // Wide Slash
            MobAttackManager.doSlash(entity, 2.0F, 13d, 2f, 0xff9b9b, Vec3.ZERO,
                    true, false, true, 1.0f, KnockBacks.toss);
        });
        ALL_LEADER_SA.add((LivingEntity entity, ServerLevel serverLevel) -> {
            // Triple Slash
            MobAttackManager.doSlash(entity, -30.0F, 7.5d, 0.25f, 0x8B0000, Vec3.ZERO,
                    true, false, true, 0.3f, KnockBacks.meteor);
            SenDims.serverScheduler.schedule(5, () -> {
                MobAttackManager.doSlash(entity, 15.0F, 9d, 1.25f, 0x6d0000, Vec3.ZERO,
                        true, false, true, 0.5f, KnockBacks.cancel);
            });
            SenDims.serverScheduler.schedule(7, () -> {
                MobAttackManager.doSlash(entity, -15.0F, 9d, 1.25f, 0x6d0000, Vec3.ZERO,
                        true, false, true, 1.1f, KnockBacks.smash);
            });

        });
        ALL_LEADER_SA.add((LivingEntity entity, ServerLevel serverLevel) -> {
            // Fang Slash
            MobAttackManager.doSlash(entity, -30.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                    true, false, true, 0.3f, KnockBacks.smash);
            MobAttackManager.doSlash(entity, 30.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                    true, false, true, 0.3f, KnockBacks.smash);

            SenDims.serverScheduler.schedule(3, () -> {
                MobAttackManager.doSlash(entity, -25.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                        true, false, true, 0.3f, KnockBacks.cancel);
                MobAttackManager.doSlash(entity, 25.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                        true, false, true, 0.3f, KnockBacks.cancel);
            });
            SenDims.serverScheduler.schedule(5, () -> {
                MobAttackManager.doSlash(entity, -15.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                        true, false, true, 0.3f, KnockBacks.smash);
                MobAttackManager.doSlash(entity, 15.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                        true, false, true, 0.3f, KnockBacks.smash);
            });
            SenDims.serverScheduler.schedule(7, () -> {
                MobAttackManager.doSlash(entity, -30.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                        true, false, true, 0.3f, KnockBacks.cancel);
                MobAttackManager.doSlash(entity, 30.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                        true, false, true, 0.3f, KnockBacks.cancel);
            });
            SenDims.serverScheduler.schedule(9, () -> {
                MobAttackManager.doSlash(entity, -45.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                        true, false, true, 0.3f, KnockBacks.smash);
                MobAttackManager.doSlash(entity, 45.0F, 6.5d, 0.4f, 0x8B0000, Vec3.ZERO,
                        true, false, true, 0.3f, KnockBacks.smash);
            });

        });
    }

    public static HashSet<EntityType<?>> DEFAULT_LEADER_SET =  new HashSet<>();

    static {
        DEFAULT_LEADER_SET.add(TFEntities.MINOSHROOM.get());
        DEFAULT_LEADER_SET.add(TFEntities.KNIGHT_PHANTOM.get());
        DEFAULT_LEADER_SET.add(TFEntities.ALPHA_YETI.get());
//        DEFAULT_LEADER_SET.add(BotaniaEntities.DOPPLEGANGER);
    }

    // Miscs
    public static int GEM_UPGRADE_COST = 2;
    public static String DISABLE_RENDER_BOARDER_STAGE = "sdbf.world_lock";
    public static final Map<ResourceLocation, Integer> HEIGHT_BOARDER_Y = new HashMap<>();

    static {
        HEIGHT_BOARDER_Y.put(Level.OVERWORLD.location(), 280);
    }

    public static void notifyPlayer(Player player, MutableComponent translatable) {
        player.sendSystemMessage(translatable);

    }

    public static void doSprintSuccessIndicators(ServerPlayer serverPlayer, int count) {
        serverPlayer.playNotifySound(SoundEvents.FIREWORK_ROCKET_LAUNCH, serverPlayer.getSoundSource(), 0.5f * count, 1.05f);
        serverPlayer.serverLevel().sendParticles(ParticleTypes.END_ROD,
                serverPlayer.getX(), serverPlayer.getY() + 0.5d, serverPlayer.getZ(),
                count * 3, 0.5d, 0.25d, 0.5d, 0.05d);
    }
}
