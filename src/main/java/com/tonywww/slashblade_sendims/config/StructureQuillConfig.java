package com.tonywww.slashblade_sendims.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StructureQuillConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CREATIVE_TAB_STRUCTURES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("structureQuill");
        CREATIVE_TAB_STRUCTURES = builder
                .comment("Structure IDs whose preconfigured quills are shown in the creative mode tab.",
                        "Tooltip translation keys are generated as structure.<namespace>.<path>.",
                        "Changing this value requires a client restart.")
                .defineList("creativeTabStructures", List.of("minecraft:stronghold"),
                        StructureQuillConfig::isValidStructureId);
        builder.pop();
        SPEC = builder.build();
    }

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

    private static boolean isValidStructureId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }
}