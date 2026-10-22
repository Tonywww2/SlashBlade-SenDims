package com.tonywww.slashblade_sendims.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicEvolutionCompatConfig;
import com.tonywww.slashblade_sendims.startup.StartupSettings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Configuration compatibility and early-read checks; never starts a game or installs a probe. */
public final class ConfigSelfTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        // Initialize a facade before the shared definition, catching static-initialization cycles.
        check(HolySwordVisualConfig.SPEC != null, "visual facade initialized first");
        check(HolySwordVisualConfig.SPEC == SenDimsConfig.SPEC && ClientPerformanceConfig.SPEC == SenDimsConfig.SPEC
                && StructureQuillConfig.SPEC == SenDimsConfig.SPEC && DraconicEvolutionCompatConfig.SPEC == SenDimsConfig.SPEC,
                "all sections share one Forge specification");
        Path directory = Files.createTempDirectory("sendims-config-check-");
        Path file = directory.resolve(StartupSettings.FILE_NAME);
        try {
            check(StartupSettings.load(file).enabled && !Files.exists(file), "early defaults never create a separate file");
            CommentedConfig values = new TomlParser().parse("");
            SenDimsConfig.SPEC.correct(values);
            check(SenDimsConfig.SPEC.isCorrect(values), "complete default schema");
            Files.writeString(file, new TomlWriter().writeToString(values));
            StartupSettings defaults = StartupSettings.load(file);
            check(defaults.enabled && defaults.opcodeNameCache && defaults.objParser && defaults.parallelObjPreparation
                    && defaults.objDiskCache && defaults.modelLocationCache && defaults.modelInputBatches && defaults.lazyConfigImages,
                    "early reader and Forge defaults agree");
            check(defaults.objCacheMiB == 128 && defaults.objMaxSourceBytes == 16 * 1024 * 1024
                    && defaults.modelLocationEntries == 262144, "startup default budgets agree");
            values.set("performance.rendering.draconicGrinderGeometry", false);
            values.set("performance.rendering.grinderCacheMiB", 5);
            values.set("performance.hud.questTrimmedTextCache", false);
            values.set("performance.neat.healthBars", "TARGETED");
            values.set("holySwordVisuals.intensity", 0.4);
            values.set("holySwordVisuals.moteBudget", 128);
            values.set("structureQuill.creativeTabStructures", List.of("minecraft:end_city", "sdbf:ae_lab"));
            values.set("draconicEvolution.targetDimension", "test:target");
            SenDimsConfig.SPEC.setConfig(values);
            check(!ClientPerformanceConfig.GRINDER_GEOMETRY.get() && ClientPerformanceConfig.GRINDER_CACHE_MIB.get() == 5,
                    "rendering facade reads unified values");
            check(!ClientPerformanceConfig.QUEST_TEXT.get() && ClientPerformanceConfig.HEALTH_BARS.get() == ClientPerformanceConfig.HealthBars.TARGETED,
                    "HUD and enum values remain local");
            check(HolySwordVisualConfig.INTENSITY.get() == 0.4 && HolySwordVisualConfig.MOTE_BUDGET.get() == 128,
                    "visual facade reads unified values");
            check(StructureQuillConfig.CREATIVE_TAB_STRUCTURES.get().equals(List.of("minecraft:end_city", "sdbf:ae_lab"))
                    && DraconicEvolutionCompatConfig.TARGET_DIMENSION.get().equals("test:target"), "structure and dimension values preserved");
            Files.writeString(file, "[startup]\nenabled=false\nobjWorkers=0x2\nmodelLocationEntries=1_024\n");
            StartupSettings disabled = StartupSettings.load(file);
            check(!disabled.opcodeNameCache && !disabled.objParser && disabled.objWorkers == 2
                    && disabled.modelLocationEntries == 1024, "real TOML syntax supported during early loading");
            Files.writeString(file, "[startup]\nobjWorkers=4294967297\nmodelWorkers=2.5\nobjCacheMiB=\"64\"\nobjParser=false\n");
            StartupSettings bounded = StartupSettings.load(file);
            check(bounded.objWorkers >= 1 && bounded.objWorkers <= 4 && bounded.modelWorkers <= 8
                    && bounded.objCacheMiB == 128 && !bounded.parallelObjPreparation && !bounded.objDiskCache,
                    "invalid types and large numbers cannot bypass budgets");
            String override = System.getProperty("sendims.startup.enabled");
            try {
                System.setProperty("sendims.startup.enabled", "false");
                check(!StartupSettings.load(file).enabled, "temporary JVM override still works");
            } finally {
                if (override == null) System.clearProperty("sendims.startup.enabled"); else System.setProperty("sendims.startup.enabled", override);
            }
            for (String arg : args) {
                CommentedConfig actual;
                try (var reader = Files.newBufferedReader(Path.of(arg))) { actual = new TomlParser().parse(reader); }
                Map<String, Object> before = leaves(actual);
                SenDimsConfig.SPEC.correct(actual);
                check(before.equals(leaves(actual)), "all manually consolidated values accepted without correction: " + arg);
                check(SenDimsConfig.SPEC.isCorrect(actual), "manually consolidated configuration satisfies Forge schema");
                System.out.println("Unified config validated: " + before.size() + " settings");
            }
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(directory); }
        System.out.println("Unified configuration checks passed: " + checks);
    }

    private static Map<String, Object> leaves(UnmodifiableConfig config) {
        Map<String, Object> values = new HashMap<>();
        collect(config, "", values);
        return values;
    }

    private static void collect(UnmodifiableConfig config, String prefix, Map<String, Object> values) {
        for (var entry : config.entrySet()) {
            String key = prefix + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig nested) collect(nested, key + ".", values);
            else values.put(key, value instanceof List<?> list ? new ArrayList<>(list) : value);
        }
    }
}
