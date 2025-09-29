package com.tonywww.slashblade_sendims;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class SBSDValues {
    // SPRINT
    public static final int SPRINT_CD = 40;
    public static final String SPRINT_CD_PATH = "sbsd.sprint.cd";
    public static final String SPRINT_SUCCESSED_PATH = "sbsd.sprint.cd_success";
    public static final int SPRINT_COST = 75;
    public static final int UNTOUCHABLE_TICK = 10;
    public static final int SPRINT_SUCCESS_AP = 300;

    // HIT
    public static final int HIT_LEADER_AP = 20;
    public static final Map<ResourceLocation, Integer> COMBO_COST_MAP = new HashMap<>();
    static {
        COMBO_COST_MAP.put(ComboStateRegistry.RAPID_SLASH.getId(), -175); // * 4 - 5
        COMBO_COST_MAP.put(ComboStateRegistry.UPPERSLASH.getId(), -400);
    }

    public static final Map<ResourceLocation, Integer> SA_COST_MAP = new HashMap<>();
    static {
        SA_COST_MAP.put(SlashArtsRegistry.VOID_SLASH.getId(), -400);
        SA_COST_MAP.put(SlashArtsRegistry.DRIVE_HORIZONTAL.getId(), -300);
        SA_COST_MAP.put(SlashArtsRegistry.DRIVE_VERTICAL.getId(), -300);
    }

    // LEADER
    public static final int MIN_SPECIAL_ATTACK_TICK = 80;
    public static final int MAX_SPECIAL_ATTACK_TICK = 160;
    public static final int PRE_N_ATTACK_TICK = 60;
    public static final int PRE_PARRY_TICK = 30;
    public static final int PARRY_TICK = 15;
    public static final int END_PARRIED_TICK = 140;
    public static final double LEADER_HP_SCALE = 4.0d;
    public static final float PARRIED_DAMAGE_SCALE = 3.0f;

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
        ALL_LEADER_SA.add(SBSDLeader::doLeaderSAQuickSLash);
        ALL_LEADER_SA.add(SBSDLeader::doLeaderSATripleSlash);
    }

    public static void notifyPlayer(Player player, MutableComponent translatable) {
        player.sendSystemMessage(translatable);

    }
}
