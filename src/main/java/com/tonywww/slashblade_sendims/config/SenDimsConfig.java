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
    public static final ForgeConfigSpec.BooleanValue RITUAL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue RITUAL_SCREEN_EFFECTS;
    public static final ForgeConfigSpec.DoubleValue RITUAL_INTENSITY;
    public static final ForgeConfigSpec.DoubleValue RITUAL_SHADER_PACK_GLOW;
    public static final ForgeConfigSpec.IntValue RITUAL_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue RITUAL_CANVAS_RESOLUTION;
    public static final ForgeConfigSpec.BooleanValue ZENITH_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ZENITH_SCREEN_EFFECTS;
    public static final ForgeConfigSpec.DoubleValue ZENITH_FLASH_INTENSITY;
    public static final ForgeConfigSpec.BooleanValue ZENITH_BLADE_ANIMATION;
    public static final ForgeConfigSpec.EnumValue<ClientPerformanceConfig.HealthBars> HEALTH_BARS;
    public static final ForgeConfigSpec.BooleanValue COMPACT_HEALTH_TEXT;
    public static final ForgeConfigSpec.BooleanValue ATTRIBUTE_ICONS;
    public static final ForgeConfigSpec.BooleanValue GRINDER_GEOMETRY;
    public static final ForgeConfigSpec.IntValue GRINDER_CACHE_MIB;
    public static final ForgeConfigSpec.BooleanValue SLASHBLADE_OBJ;
    public static final ForgeConfigSpec.IntValue CAMPFIRE_SMOKE_DISTANCE;
    public static final ForgeConfigSpec.IntValue SPAWNER_CLIENT_TICK_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue XAERO_WORLD_MAP_WRITE_RATE;
    public static final ForgeConfigSpec.BooleanValue QUEST_BACKGROUND;
    public static final ForgeConfigSpec.BooleanValue QUEST_TEXT;
    public static final ForgeConfigSpec.BooleanValue DRACONIC_ENERGY_CORE_BOUNDS;
    public static final ForgeConfigSpec.BooleanValue DRACONIC_TRANSPARENT_PASS;
    public static final ForgeConfigSpec.BooleanValue DRACONIC_MODEL_GEOMETRY;
    public static final ForgeConfigSpec.BooleanValue ID_CABLE_LIGHT;
    public static final ForgeConfigSpec.BooleanValue ID_CABLE_OVERLAY;
    public static final ForgeConfigSpec.BooleanValue YSM_PLAYER_SHADOWS;
    public static final ForgeConfigSpec.BooleanValue TF_CASTLE_PORTAL;
    public static final ForgeConfigSpec.ConfigValue<String> TF_PEDESTAL_ADVANCEMENT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TF_BOSS_SPAWNER_GATES;
    public static final ForgeConfigSpec.IntValue TF_ACID_RAIN_SLOWNESS;
    public static final ForgeConfigSpec.IntValue TF_ACID_RAIN_MINING_FATIGUE;
    public static final ForgeConfigSpec.IntValue TF_ACID_RAIN_EFFECT_TICKS;
    public static final ForgeConfigSpec.BooleanValue TF_THORNS_BREAKABLE;

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
        CAMPFIRE_SMOKE_DISTANCE = builder.comment("Campfires farther than this many blocks from the camera spawn no smoke. Vanilla campfire smoke ignores particle distance. Zero keeps vanilla.")
                .defineInRange("campfireSmokeDistance", 64, 0, 512);
        SPAWNER_CLIENT_TICK_DISTANCE = builder.comment("Spawners farther than this many blocks from the camera skip their client tick (mob spin and flame particles). Vanilla draws spawners within 64 blocks and spawns their particles within 32. Zero keeps vanilla.")
                .defineInRange("spawnerClientTickDistance", 64, 0, 512);
        XAERO_WORLD_MAP_WRITE_RATE = builder.comment("Xaero's World Map write speed and per-frame write budget multiplier. The minimap is unaffected. 1.0 keeps upstream behaviour.")
                .defineInRange("xaeroWorldMapWriteRate", 1.0, 0.1, 1.0);
        DRACONIC_ENERGY_CORE_BOUNDS = builder.comment("Give active energy cores a finite render box covering the core, stabilizers and beams, so cores outside the view are culled. The build guide keeps the infinite box.")
                .define("draconicEnergyCoreBounds", true);
        DRACONIC_TRANSPARENT_PASS = builder.comment("Brandon's Core transparent pass checks the block entity renderer type before computing render bounds; block entities without a transparent renderer skip the bounds.")
                .define("draconicTransparentPass", true);
        DRACONIC_MODEL_GEOMETRY = builder.comment("Write energy core, stabilizer and pylon meshes with CCL's own per-vertex math and format order, without the generic per-vertex pipeline, and reuse the pylon shell render type. False restores CCL rendering.")
                .define("draconicModelGeometry", true);
        ID_CABLE_LIGHT = builder.comment("Integrated Dynamics cable light emission reads the cable's light levels once instead of six capability lookups. Applies to the client and its integrated server.")
                .define("integratedDynamicsCableLight", true);
        ID_CABLE_OVERLAY = builder.comment("Skip the Integrated Dynamics cable block entity renderer when no part on the cable has an overlay renderer (display panels, omni-directional connectors).")
                .define("integratedDynamicsCableOverlay", true);
        YSM_PLAYER_SHADOWS = builder.comment("Render players in the shader shadow pass while Yes Steve Model is installed. False skips them there; without YSM or a shader pack nothing changes.")
                .define("ysmPlayerShadows", false);
        builder.pop();
        builder.push("hud");
        QUEST_BACKGROUND = builder.comment("Batch UIQuest background slices; preserve the original stepped gradient and text/item ordering.")
                .define("questBackgroundBatching", true);
        QUEST_TEXT = builder.comment("Cache immutable trimmed quest text by font, resource generation and width. Quest progress is never cached.")
                .define("questTrimmedTextCache", true);
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

        builder.push("ritualVisuals");
        RITUAL_ENABLED = builder.comment("Shader visuals for summoning, portal, map teleport and bedrock breaker rituals. Client visuals only.")
                .define("enabled", true);
        RITUAL_SCREEN_EFFECTS = builder.comment("Screen effects: teleport vortex, arrival flash and summon ripple.")
                .define("screenEffects", true);
        RITUAL_INTENSITY = builder.comment("Overall brightness of ritual visuals.")
                .defineInRange("intensity", 1.0, 0.25, 1.5);
        RITUAL_SHADER_PACK_GLOW = builder.comment("Baked halo strength while an Oculus shader pack is active; lower it if a shader pack's bloom overexposes the effects.")
                .defineInRange("shaderPackGlow", 1.0, 0.0, 1.0);
        RITUAL_MAX_DISTANCE = builder.comment("Maximum distance for ritual visuals. Summon sky beams are visible up to 160 blocks.")
                .defineInRange("maxDistance", 128, 32, 256);
        RITUAL_CANVAS_RESOLUTION = builder.comment("Edge length of the magic circle canvas: 512, 1024 or 2048.")
                .defineInRange("canvasResolution", 1024, 512, 2048);
        builder.pop();

        builder.push("zenithVisuals");
        ZENITH_ENABLED = builder.comment("Visuals for the True Judgement Cut Slash Art (world slashes and screen effects). Client visuals only; damage is unaffected.")
                .define("enabled", true);
        ZENITH_SCREEN_EFFECTS = builder.comment("Screen layer: cut lines, frozen frame and shattering. Shown to every player within 64 blocks of the cast.")
                .define("screenEffects", true);
        ZENITH_FLASH_INTENSITY = builder.comment("Strength of the white flashes. Lower it, or set 0, if bright flashes are uncomfortable.")
                .defineInRange("flashIntensity", 1.0, 0.0, 1.0);
        ZENITH_BLADE_ANIMATION = builder.comment("Animate the shader-drawn rift on the Zenith Rift blade, guard and sheath. When off, one still frame is drawn.")
                .define("bladeAnimation", true);
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

        builder.push("twilightProgression");
        TF_CASTLE_PORTAL = builder.comment("Send players entering the Twilight Forest to the portal in the nearest Final Castle, and send them back to the portal they came from.")
                .define("finalCastlePortal", true);
        TF_PEDESTAL_ADVANCEMENT = builder.comment("Advancement a nearby player needs before a trophy pedestal removes the stronghold shields. Twilight Forest default: twilightforest:progress_lich.")
                .define("trophyPedestalAdvancement", "twilightforest:progress_naga", SenDimsConfig::isResourceId);
        TF_BOSS_SPAWNER_GATES = builder.comment("Boss spawners only count nearby players who have the advancement. Format: <entity id>=<advancement id>.")
                .defineList("bossSpawnerGates", List.of(
                        "twilightforest:minoshroom=twilightforest:mazebreaker",
                        "twilightforest:hydra=twilightforest:progress_labyrinth",
                        "twilightforest:naga=twilightforest:progress_hydra",
                        "twilightforest:knight_phantom=twilightforest:progress_trophy_pedestal",
                        "twilightforest:ur_ghast=twilightforest:progress_knights",
                        "twilightforest:lich=twilightforest:progress_ur_ghast",
                        "twilightforest:alpha_yeti=twilightforest:progress_lich",
                        "twilightforest:snow_queen=twilightforest:progress_yeti"
                ), SenDimsConfig::isSpawnerGate);
        TF_ACID_RAIN_SLOWNESS = builder.comment("Slowness amplifier added by the acid rain biome penalty. -1 disables it.")
                .defineInRange("acidRainSlowness", 1, -1, 9);
        TF_ACID_RAIN_MINING_FATIGUE = builder.comment("Mining Fatigue amplifier added by the acid rain biome penalty. -1 disables it.")
                .defineInRange("acidRainMiningFatigue", 1, -1, 9);
        TF_ACID_RAIN_EFFECT_TICKS = builder.comment("Duration of those effects in ticks. The penalty is refreshed every second.")
                .defineInRange("acidRainEffectTicks", 100, 20, 1200);
        TF_THORNS_BREAKABLE = builder.comment("Players can break Twilight Forest thorns instead of making them grow more thorns. Hardness is set by the modpack.")
                .define("thornsBreakable", true);
        builder.pop();
        SPEC = builder.build();
    }

    private SenDimsConfig() {}

    private static boolean isResourceId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }

    private static boolean isSpawnerGate(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        int split = entry.indexOf('=');
        return split > 0 && isResourceId(entry.substring(0, split)) && isResourceId(entry.substring(split + 1));
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
