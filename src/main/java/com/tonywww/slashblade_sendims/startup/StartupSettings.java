package com.tonywww.slashblade_sendims.startup;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Immutable early snapshot of [startup] in the same file Forge later loads. */
public final class StartupSettings {
    public static final String FILE_NAME = "slashblade_sendims.toml";
    public static final StartupSettings INSTANCE = load(Path.of("config", FILE_NAME));

    public final boolean enabled;
    public final boolean opcodeNameCache;
    public final boolean objParser;
    public final boolean parallelObjPreparation;
    public final boolean objDiskCache;
    public final boolean modelLocationCache;
    public final boolean modelInputBatches;
    public final boolean lazyConfigImages;
    public final int modelWorkers;
    public final int objWorkers;
    public final int objCacheMiB;
    public final int objMaxSourceBytes;
    public final int modelLocationEntries;

    private StartupSettings(UnmodifiableConfig values) {
        enabled = bool(values, "enabled", true);
        opcodeNameCache = enabled && bool(values, "opcodeNameCache", true);
        objParser = enabled && bool(values, "objParser", true);
        parallelObjPreparation = objParser && bool(values, "parallelObjPreparation", true);
        objDiskCache = objParser && bool(values, "objDiskCache", true);
        modelLocationCache = enabled && bool(values, "modelLocationCache", true);
        modelInputBatches = enabled && bool(values, "modelInputBatches", true);
        lazyConfigImages = enabled && bool(values, "lazyConfigImages", true);
        int requestedModels = integer(values, "modelWorkers", 0, 0, 32);
        modelWorkers = requestedModels == 0 ? Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() / 2)) : requestedModels;
        int automatic = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 4));
        int requested = integer(values, "objWorkers", 0, 0, 8);
        objWorkers = requested == 0 ? automatic : requested;
        objCacheMiB = integer(values, "objCacheMiB", 128, 0, 1024);
        objMaxSourceBytes = integer(values, "objMaxSourceMiB", 16, 1, 64) * 1024 * 1024;
        modelLocationEntries = integer(values, "modelLocationEntries", 262144, 0, 1048576);
    }

    public static StartupSettings load(Path path) {
        if (Files.isRegularFile(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Object section = new TomlParser().parse(reader).get("startup");
                if (section instanceof UnmodifiableConfig values) return new StartupSettings(values);
            } catch (IOException | RuntimeException error) {
                System.err.println("[SenDims Startup] Cannot read [startup]; using defaults: " + error.getMessage());
            }
        }
        // Forge creates the complete unified config later; the early reader never writes files.
        return new StartupSettings(null);
    }

    private static Object value(UnmodifiableConfig values, String key) {
        String override = System.getProperty("sendims.startup." + key);
        return override != null ? override.trim() : values == null ? null : values.get(key);
    }

    private static boolean bool(UnmodifiableConfig values, String key, boolean fallback) {
        Object value = value(values, key);
        if (value instanceof Boolean flag) return flag;
        if (System.getProperty("sendims.startup." + key) != null && value instanceof String text) {
            if ("true".equalsIgnoreCase(text)) return true;
            if ("false".equalsIgnoreCase(text)) return false;
        }
        return fallback;
    }

    private static int integer(UnmodifiableConfig values, String key, int fallback, int minimum, int maximum) {
        Object value = value(values, key);
        long parsed;
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            parsed = ((Number) value).longValue();
        } else if (System.getProperty("sendims.startup." + key) != null && value instanceof String text) {
            try { parsed = Long.parseLong(text); }
            catch (NumberFormatException invalid) { return fallback; }
        } else return fallback;
        return parsed >= minimum && parsed <= maximum ? (int) parsed : fallback;
    }
}
