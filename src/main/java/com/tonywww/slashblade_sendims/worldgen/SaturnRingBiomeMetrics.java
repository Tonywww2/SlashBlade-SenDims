package com.tonywww.slashblade_sendims.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

public class SaturnRingBiomeMetrics {

    public static class TerrainMetrics {
        public final float thickness;
        public final float weightModifier;
        public final float roughness;

        public TerrainMetrics(float thickness, float weightModifier, float roughness) {
            this.thickness = thickness;
            this.weightModifier = weightModifier;
            this.roughness = roughness;
        }
    }

    private static final TerrainMetrics EMPTY = new TerrainMetrics(0.0f, 1.0f, 0.0f);
    private static final Map<ResourceKey<Biome>, TerrainMetrics> METRICS_MAP = new HashMap<>();

    static {
        METRICS_MAP.put(SaturnRingBiomes.VOID_RING, EMPTY);
        METRICS_MAP.put(SaturnRingBiomes.INNER_FADING_RING, new TerrainMetrics(3.0f, 0.7f, 1.5f));
        METRICS_MAP.put(SaturnRingBiomes.INNER_SPARSE_RING, new TerrainMetrics(6.0f, 1.0f, 3.0f));
        METRICS_MAP.put(SaturnRingBiomes.BASE_RING, new TerrainMetrics(14.0f, 1.0f, 5.0f));
        METRICS_MAP.put(SaturnRingBiomes.HIGH_DENSITY_RING, new TerrainMetrics(24.0f, 1.4f, 10.0f));
        METRICS_MAP.put(SaturnRingBiomes.TRANSITION_WALL_RING, new TerrainMetrics(56.0f, 0.0f, 0.0f));
        METRICS_MAP.put(SaturnRingBiomes.OUTER_SPARSE_RING, new TerrainMetrics(4.0f, 0.8f, 2.0f));
    }

    public static TerrainMetrics getMetrics(Holder<Biome> biomeHolder) {
        return biomeHolder.unwrapKey().map(METRICS_MAP::get).orElse(EMPTY);
    }
}
