package com.tonywww.slashblade_sendims.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Typed access to the shared local configuration. */
public final class ClientPerformanceConfig {
    public enum HealthBars { ALL, DAMAGED_OR_TARGETED, TARGETED }

    public static final ForgeConfigSpec SPEC = SenDimsConfig.SPEC;
    public static final ForgeConfigSpec.EnumValue<HealthBars> HEALTH_BARS = SenDimsConfig.HEALTH_BARS;
    public static final ForgeConfigSpec.BooleanValue COMPACT_HEALTH_TEXT = SenDimsConfig.COMPACT_HEALTH_TEXT;
    public static final ForgeConfigSpec.BooleanValue ATTRIBUTE_ICONS = SenDimsConfig.ATTRIBUTE_ICONS;
    public static final ForgeConfigSpec.BooleanValue GRINDER_GEOMETRY = SenDimsConfig.GRINDER_GEOMETRY;
    public static final ForgeConfigSpec.IntValue GRINDER_CACHE_MIB = SenDimsConfig.GRINDER_CACHE_MIB;
    public static final ForgeConfigSpec.BooleanValue SLASHBLADE_OBJ = SenDimsConfig.SLASHBLADE_OBJ;
    public static final ForgeConfigSpec.IntValue CAMPFIRE_SMOKE_DISTANCE = SenDimsConfig.CAMPFIRE_SMOKE_DISTANCE;
    public static final ForgeConfigSpec.IntValue SPAWNER_CLIENT_TICK_DISTANCE = SenDimsConfig.SPAWNER_CLIENT_TICK_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue XAERO_WORLD_MAP_WRITE_RATE = SenDimsConfig.XAERO_WORLD_MAP_WRITE_RATE;
    public static final ForgeConfigSpec.BooleanValue QUEST_BACKGROUND = SenDimsConfig.QUEST_BACKGROUND;
    public static final ForgeConfigSpec.BooleanValue QUEST_TEXT = SenDimsConfig.QUEST_TEXT;
    public static final ForgeConfigSpec.BooleanValue DRACONIC_ENERGY_CORE_BOUNDS = SenDimsConfig.DRACONIC_ENERGY_CORE_BOUNDS;
    public static final ForgeConfigSpec.BooleanValue DRACONIC_TRANSPARENT_PASS = SenDimsConfig.DRACONIC_TRANSPARENT_PASS;
    public static final ForgeConfigSpec.BooleanValue DRACONIC_MODEL_GEOMETRY = SenDimsConfig.DRACONIC_MODEL_GEOMETRY;
    public static final ForgeConfigSpec.BooleanValue ID_CABLE_LIGHT = SenDimsConfig.ID_CABLE_LIGHT;
    public static final ForgeConfigSpec.BooleanValue ID_CABLE_OVERLAY = SenDimsConfig.ID_CABLE_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue YSM_PLAYER_SHADOWS = SenDimsConfig.YSM_PLAYER_SHADOWS;

    private ClientPerformanceConfig() {}
}
