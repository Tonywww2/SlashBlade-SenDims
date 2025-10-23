package com.tonywww.slashblade_sendims;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class SBSDValues {
    public static final int CANCELED_CD = 8;
    // SPRINT
    public static final int SPRINT_CD = 50;
    public static final String SPRINT_CD_PATH = "sbsd.sprint.cd";
    public static final String SPRINT_SUCCESSED_PATH = "sbsd.sprint.cd_success";
    public static final int SPRINT_COST = -150;
    public static final int UNTOUCHABLE_TICK = 10;
    public static final int SPRINT_SUCCESS_AP = 300;

    // HIT
    public static final int HIT_LEADER_AP = 20;
    public static final Map<ResourceLocation, Integer> COMBO_COST_MAP = new HashMap<>();
    static {
        COMBO_COST_MAP.put(ComboStateRegistry.RAPID_SLASH.getId(), -800);
        COMBO_COST_MAP.put(ComboStateRegistry.UPPERSLASH.getId(), -400);
    }

//    public static final Map<ResourceLocation, Integer> SA_COST_MAP = new HashMap<>();
//    static {
//        SA_COST_MAP.put(SlashArtsRegistry.VOID_SLASH.getId(), -400);
//        SA_COST_MAP.put(SlashArtsRegistry.DRIVE_HORIZONTAL.getId(), -300);
//        SA_COST_MAP.put(SlashArtsRegistry.DRIVE_VERTICAL.getId(), -300);
//        SA_COST_MAP.put(SlashArtsRegistry.CIRCLE_SLASH.getId(), -350);
//        SA_COST_MAP.put(SlashArtsRegistry.JUDGEMENT_CUT.getId(), -300);
//        SA_COST_MAP.put(SlashArtsRegistry.PIERCING.getId(), -300);
//        SA_COST_MAP.put(SlashArtsRegistry.SAKURA_END.getId(), -300);
//        SA_COST_MAP.put(SlashArtsRegistry.WAVE_EDGE.getId(), -300);
//
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:blaze_reborn"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:circle_slash"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:corpse_piler"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:cross_slash"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:cross_wave_slash"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:ground_pound"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:heavens_thirteen_slashes"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:ice_thunder_sword"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:law_of_regression"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:none"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:quick_draw"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:seppuku"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:storm_bias"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:swallow_return"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:thermal_wave_beam"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:transient_moonlight"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade:waterfowl_dance"), -300);
//
//        SA_COST_MAP.put(ResourceLocation.parse("foxextra:sakura_endex"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("foxextra:thrust"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("foxextra:void_slash_plus"), -300);
//
//        SA_COST_MAP.put(ResourceLocation.parse("last_smith:fushigiri"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("last_smith:iai_cross"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("last_smith:sakura_blistering_swords"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("last_smith:transmigration_slash"), -300);
//
//        SA_COST_MAP.put(ResourceLocation.parse("pseudoedge_break_dawn:black_hole"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("pseudoedge_break_dawn:blackslash"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("pseudoedge_break_dawn:dragon_boost"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("pseudoedge_break_dawn:kingblade"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("pseudoedge_break_dawn:thrust_eex"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("pseudoedge_break_dawn:thrust_slash"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("pseudoedge_break_dawn:thrust_slash_ex"), -300);
//
//        SA_COST_MAP.put(ResourceLocation.parse("redtassel:that_go_go_go"), -300);
//
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:burning_dances"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:cold_drive"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:dark_cuts"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:dark_cuts_ex"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:explosive_dawn"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:fire_dance"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:for_life"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:for_life_drive"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:illusion_drive"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:lightinggwaves"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:rainbow_drives"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:sakura_drive"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:sakura_wave_edge"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:super_blood_cuts"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:terrifyingwaves"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:wave_edge_supers"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("sjap_adder:xross_thunder"), -300);
//
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade_addon:fire_spiral"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade_addon:gale_swords"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade_addon:lighting_swords"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade_addon:rapid_blistering_swords"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade_addon:spiral_edge"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade_addon:water_drive"), -300);
//        SA_COST_MAP.put(ResourceLocation.parse("slashblade_sendims:frenzied_burst"), -300);
//
//    }

    // LEADER
    public static final int MIN_SPECIAL_ATTACK_TICK = 80;
    public static final int MAX_SPECIAL_ATTACK_TICK = 160;
    public static final int PRE_N_ATTACK_TICK = 60;
    public static final int PRE_PARRY_TICK = 30;
    public static final int PARRY_TICK = 15;
    public static final int END_PARRIED_TICK = 140;
    public static final double LEADER_HP_SCALE = 5.0d;
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
        ALL_LEADER_SA.add(SBSDLeader::doLeaderSAWideSLash);
        ALL_LEADER_SA.add(SBSDLeader::doLeaderSATripleSlash);
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
