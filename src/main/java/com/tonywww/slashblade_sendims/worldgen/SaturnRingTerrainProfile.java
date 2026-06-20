package com.tonywww.slashblade_sendims.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public record SaturnRingTerrainProfile(
        Holder<Biome> biomeHolder,
        double expectedThickness,
        int topY,
        int bottomY
) {
    public static SaturnRingTerrainProfile sample(int x, int z, Climate.Sampler sampler, BiomeSource biomeSource,
                                                  SimplexNoise heightNoise, SimplexNoise thicknessNoise, int ringYCenter) {
        Holder<Biome> biomeHolder = biomeSource.getNoiseBiome(x >> 2, 0, z >> 2, sampler);

        if (biomeHolder.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            double expectedThickness = SaturnRingBiomeMetrics.getMetrics(biomeHolder).thickness;
            int halfThickness = (int) Math.round(expectedThickness / 2.0);
            return new SaturnRingTerrainProfile(
                    biomeHolder,
                    expectedThickness,
                    ringYCenter + halfThickness,
                    ringYCenter - halfThickness
            );
        }

        SaturnRingTerrainSampler.SampledTerrain terrain = SaturnRingTerrainSampler.sampleTerrain(x, z, sampler, biomeSource);
        double expectedThickness = applyThicknessNoise(x, z, biomeHolder, terrain.thickness(), thicknessNoise);

        if (expectedThickness <= 0.5) {
            return new SaturnRingTerrainProfile(biomeHolder, expectedThickness, ringYCenter, ringYCenter);
        }

        int halfThickness = (int) Math.round(expectedThickness / 2.0);
        double topOffset = heightNoise.getValue(x * 0.005, z * 0.005) * terrain.roughness();
        double bottomOffset = heightNoise.getValue(x * 0.005 + 1000, z * 0.005 + 1000) * terrain.roughness();

        return new SaturnRingTerrainProfile(
                biomeHolder,
                expectedThickness,
                ringYCenter + halfThickness + (int) Math.round(topOffset),
                ringYCenter - halfThickness + (int) Math.round(bottomOffset)
        );
    }

    private static double applyThicknessNoise(int x, int z, Holder<Biome> biomeHolder, double baseThickness, SimplexNoise thicknessNoise) {
        if (baseThickness <= 0.5) {
            return baseThickness;
        }

        double noise = thicknessNoise.getValue(x * 0.003, z * 0.003);
        double modifier = 0.25;
        if (biomeHolder.is(SaturnRingBiomes.INNER_FADING_RING)) {
            modifier = 0.55;
        } else if (biomeHolder.is(SaturnRingBiomes.INNER_SPARSE_RING) || biomeHolder.is(SaturnRingBiomes.OUTER_SPARSE_RING)) {
            modifier = 0.40;
        } else if (biomeHolder.is(SaturnRingBiomes.HIGH_DENSITY_RING)) {
            modifier = 0.30;
        }

        return Math.max(0.0, baseThickness * (1.0 + noise * modifier));
    }

    public boolean hasTerrain() {
        return expectedThickness > 0.5;
    }

    public boolean containsY(int y) {
        return hasTerrain() && y >= bottomY && y <= topY;
    }
}
