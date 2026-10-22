package com.tonywww.slashblade_sendims.compat.draconicevolution;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public final class DraconicEvolutionCompatConfig {
    public static final ResourceLocation DEFAULT_TARGET_DIMENSION =
            ResourceLocation.fromNamespaceAndPath("sdbf", "inside_the_end");
    public static final ForgeConfigSpec SPEC = com.tonywww.slashblade_sendims.config.SenDimsConfig.SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> TARGET_DIMENSION = com.tonywww.slashblade_sendims.config.SenDimsConfig.TARGET_DIMENSION;

    private DraconicEvolutionCompatConfig() {
    }

    public static ResourceLocation targetDimension() {
        ResourceLocation configured = ResourceLocation.tryParse(TARGET_DIMENSION.get());
        return configured != null ? configured : DEFAULT_TARGET_DIMENSION;
    }

}
