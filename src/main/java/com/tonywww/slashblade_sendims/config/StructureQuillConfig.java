package com.tonywww.slashblade_sendims.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StructureQuillConfig {
    public static final ForgeConfigSpec SPEC = com.tonywww.slashblade_sendims.config.SenDimsConfig.SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CREATIVE_TAB_STRUCTURES = com.tonywww.slashblade_sendims.config.SenDimsConfig.CREATIVE_TAB_STRUCTURES;

    private StructureQuillConfig() {
    }

    public static List<ResourceLocation> creativeTabStructures() {
        Set<ResourceLocation> structures = new LinkedHashSet<>();
        for (String configuredId : CREATIVE_TAB_STRUCTURES.get()) {
            ResourceLocation structureId = ResourceLocation.tryParse(configuredId);
            if (structureId != null) {
                structures.add(structureId);
            }
        }
        return List.copyOf(structures);
    }

}
