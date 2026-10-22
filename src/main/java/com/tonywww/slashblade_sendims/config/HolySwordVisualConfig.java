package com.tonywww.slashblade_sendims.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Typed access to the shared local configuration. */
public final class HolySwordVisualConfig {
    public static final ForgeConfigSpec SPEC = SenDimsConfig.SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED = SenDimsConfig.HOLY_SWORD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SKY_LIGHT = SenDimsConfig.HOLY_SWORD_SKY_LIGHT;
    public static final ForgeConfigSpec.DoubleValue INTENSITY = SenDimsConfig.HOLY_SWORD_INTENSITY;
    public static final ForgeConfigSpec.IntValue MOTE_BUDGET = SenDimsConfig.HOLY_SWORD_MOTE_BUDGET;
    public static final ForgeConfigSpec.IntValue DETAIL_DISTANCE = SenDimsConfig.HOLY_SWORD_DETAIL_DISTANCE;

    private HolySwordVisualConfig() {}
}
