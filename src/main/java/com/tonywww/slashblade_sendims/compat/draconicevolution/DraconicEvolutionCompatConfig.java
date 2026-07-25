package com.tonywww.slashblade_sendims.compat.draconicevolution;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public final class DraconicEvolutionCompatConfig {
    public static final ResourceLocation DEFAULT_TARGET_DIMENSION =
            ResourceLocation.fromNamespaceAndPath("sdbf", "inside_the_end");
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> TARGET_DIMENSION;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("draconicEvolution");
        TARGET_DIMENSION = builder
                .comment("The only dimension where Draconic Evolution chaos islands may affect world generation.",
                        "Changing this value requires a server restart.")
                .define("targetDimension", DEFAULT_TARGET_DIMENSION.toString(),
                        DraconicEvolutionCompatConfig::isValidDimensionId);
        builder.pop();
        SPEC = builder.build();
    }

    private DraconicEvolutionCompatConfig() {
    }

    public static ResourceLocation targetDimension() {
        ResourceLocation configured = ResourceLocation.tryParse(TARGET_DIMENSION.get());
        return configured != null ? configured : DEFAULT_TARGET_DIMENSION;
    }

    private static boolean isValidDimensionId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }
}