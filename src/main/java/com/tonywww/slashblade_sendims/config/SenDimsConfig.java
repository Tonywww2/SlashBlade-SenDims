package com.tonywww.slashblade_sendims.config;

import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

/** One local COMMON config; client preferences are not synchronized from a server. */
public final class SenDimsConfig {
    public static final String FILE_NAME = StartupSettings.FILE_NAME;
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> TARGET_DIMENSION;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CREATIVE_TAB_STRUCTURES;
    public static final ForgeConfigSpec.BooleanValue HOLY_SWORD_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HOLY_SWORD_SKY_LIGHT;
    public static final ForgeConfigSpec.DoubleValue HOLY_SWORD_INTENSITY;
    public static final ForgeConfigSpec.IntValue HOLY_SWORD_MOTE_BUDGET;
    public static final ForgeConfigSpec.IntValue HOLY_SWORD_DETAIL_DISTANCE;
    public static final ForgeConfigSpec.EnumValue<ClientPerformanceConfig.HealthBars> HEALTH_BARS;
    public static final ForgeConfigSpec.BooleanValue COMPACT_HEALTH_TEXT;
    public static final ForgeConfigSpec.BooleanValue ATTRIBUTE_ICONS;
    public static final ForgeConfigSpec.BooleanValue GRINDER_GEOMETRY;
    public static final ForgeConfigSpec.IntValue GRINDER_CACHE_MIB;
    public static final ForgeConfigSpec.BooleanValue SLASHBLADE_OBJ;
    public static final ForgeConfigSpec.BooleanValue MINIMAP_FILTERS;
    public static final ForgeConfigSpec.BooleanValue QUEST_BACKGROUND;
    public static final ForgeConfigSpec.BooleanValue QUEST_TEXT;
    public static final ForgeConfigSpec.BooleanValue FIXED_BUFFER_VIEWS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        startup(builder);

        builder.push("performance");
        builder.push("neat");
        HEALTH_BARS = builder.comment("Additional Neat visibility filter. Existing Neat filters still apply.")
                .defineEnum("healthBars", ClientPerformanceConfig.HealthBars.ALL);
        COMPACT_HEALTH_TEXT = builder.comment("Hide duplicate maximum HP and percentage fields; keep current HP.")
                .define("compactHealthText", false);
        ATTRIBUTE_ICONS = builder.comment("Show Neat mob-type icons. Armor remains controlled by Neat's showArmor.")
                .define("attributeIcons", true);
        builder.pop();
        builder.push("rendering");
        GRINDER_GEOMETRY = builder.comment("Cache static grinder sword geometry and streamline animated submission. False restores CCL rendering.")
                .define("draconicGrinderGeometry", true);
        GRINDER_CACHE_MIB = builder.comment("Hard geometry-array budget per grinder renderer, including all idle poses. Zero uses upstream rendering.")
                .defineInRange("grinderCacheMiB", 2, 0, 16);
        SLASHBLADE_OBJ = builder.comment("Reuse OBJ face math scratch data. Live vertices, UV, light and render override events are preserved.")
                .define("slashbladeObjSubmission", true);
        builder.pop();
        builder.push("hud");
        MINIMAP_FILTERS = builder.comment("Reuse consecutive identical FTB entity-icon texture filter writes inside one HUD pass.")
                .define("minimapIconFilters", true);
        QUEST_BACKGROUND = builder.comment("Batch UIQuest background slices; preserve the original stepped gradient and text/item ordering.")
                .define("questBackgroundBatching", true);
        QUEST_TEXT = builder.comment("Cache immutable trimmed quest text by font, resource generation and width. Quest progress is never cached.")
                .define("questTrimmedTextCache", true);
        builder.pop();
        builder.push("buffers");
        FIXED_BUFFER_VIEWS = builder.comment("Reuse bounded immutable views of ImmediatelyFast fixed buffers. No buffer pooling or flush reordering.")
                .define("immediatelyFastFixedBufferViews", true);
        builder.pop(2);

        builder.push("holySwordVisuals");
        HOLY_SWORD_ENABLED = builder.comment("Enable Caliburn/Excalibur visual effects. Client visuals only.").define("enabled", true);
        HOLY_SWORD_SKY_LIGHT = builder.comment("Show ceiling-clipped sword curtains and sky pillars.").define("skyLight", true);
        HOLY_SWORD_INTENSITY = builder.comment("Effect opacity. Standard depth-tested emissive materials support shader packs.")
                .defineInRange("intensity", 1.0, 0.15, 1.5);
        HOLY_SWORD_MOTE_BUDGET = builder.comment("Maximum motes drawn across all holy sword casts. No dynamic lights.")
                .defineInRange("moteBudget", 768, 0, 768);
        HOLY_SWORD_DETAIL_DISTANCE = builder.comment("Beyond this distance render only the main light silhouettes.")
                .defineInRange("detailDistance", 48, 16, 96);
        builder.pop();

        builder.push("structureQuill");
        CREATIVE_TAB_STRUCTURES = builder.comment("Structure IDs whose preconfigured quills are shown in the creative mode tab.",
                        "Tooltip translation keys are generated as structure.<namespace>.<path>.", "Changing this value requires a client restart.")
                .defineList("creativeTabStructures", List.of("minecraft:stronghold"), SenDimsConfig::isResourceId);
        builder.pop();

        builder.push("draconicEvolution");
        TARGET_DIMENSION = builder.comment("The only dimension where Draconic Evolution chaos islands may affect world generation.",
                        "Changing this value requires a server restart.")
                .define("targetDimension", "sdbf:inside_the_end", SenDimsConfig::isResourceId);
        builder.pop();
        SPEC = builder.build();
    }

    private SenDimsConfig() {}

    private static boolean isResourceId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }

    private static void startup(ForgeConfigSpec.Builder builder) {
        builder.comment("Read before Forge config loading. All startup changes require a full game restart.")
                .push("startup");
        builder.worldRestart().comment("Enable this whole group. -Dsendims.startup.enabled=false temporarily overrides it.")
                .define("enabled", true);
        builder.worldRestart().comment("Cache Mixin opcode names using an already installed agent.").define("opcodeNameCache", true);
        builder.worldRestart().comment("Use the low-allocation OBJ parser with upstream syntax fallback.").define("objParser", true);
        builder.worldRestart().comment("Prepare OBJ models asynchronously on the existing game executor.").define("parallelObjPreparation", true);
        builder.worldRestart().comment("0 automatically chooses at most four OBJ preparation jobs.").defineInRange("objWorkers", 0, 0, 8);
        builder.worldRestart().comment("Cache parsed OBJ data on disk, keyed by resource content.").define("objDiskCache", true);
        builder.worldRestart().comment("Disk cache budget in MiB; 0 disables cache reads and writes.").defineInRange("objCacheMiB", 128, 0, 1024);
        builder.worldRestart().comment("Larger OBJ sources use the original parser with serialized fallback.").defineInRange("objMaxSourceMiB", 16, 1, 64);
        builder.worldRestart().comment("Reuse model locations during model preparation.").define("modelLocationCache", true);
        builder.worldRestart().comment("Maximum model-location entries; all are released after preparation.").defineInRange("modelLocationEntries", 262144, 0, 1048576);
        builder.worldRestart().comment("Batch model JSON input tasks on the existing game executor.").define("modelInputBatches", true);
        builder.worldRestart().comment("0 automatically chooses at most eight jobs per JSON input category.").defineInRange("modelWorkers", 0, 0, 32);
        builder.worldRestart().comment("Defer Sounds config previews on startup; keep quest HUD images eager.").define("lazyConfigImages", true);
        builder.pop();
    }
}
