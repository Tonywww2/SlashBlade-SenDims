package com.tonywww.slashblade_sendims.performance;

import java.util.Map;

/** Only loaded IDs are stored, so arbitrary missing-ID queries cannot grow the cache. */
public final class LoadedModSnapshot {
    private final Map<String, String> versions;

    public LoadedModSnapshot(Map<String, String> versions) {
        this.versions = Map.copyOf(versions);
    }

    public boolean isLoaded(String id) {
        return id != null && versions.containsKey(id);
    }

    public String version(String id) {
        return id == null ? "unknown" : versions.getOrDefault(id, "unknown");
    }
}
