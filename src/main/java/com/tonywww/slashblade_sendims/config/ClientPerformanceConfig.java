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
    public static final ForgeConfigSpec.BooleanValue MINIMAP_FILTERS = SenDimsConfig.MINIMAP_FILTERS;
    public static final ForgeConfigSpec.BooleanValue QUEST_BACKGROUND = SenDimsConfig.QUEST_BACKGROUND;
    public static final ForgeConfigSpec.BooleanValue QUEST_TEXT = SenDimsConfig.QUEST_TEXT;
    public static final ForgeConfigSpec.BooleanValue FIXED_BUFFER_VIEWS = SenDimsConfig.FIXED_BUFFER_VIEWS;

    private ClientPerformanceConfig() {}
}
