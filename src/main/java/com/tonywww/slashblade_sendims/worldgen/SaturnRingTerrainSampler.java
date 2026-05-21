package com.tonywww.slashblade_sendims.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * 参考了 Bumblezone 的 BiomeInfluencedNoiseSampler。
 * 用于中心点半径内采样群系，加权计算出平滑过渡的土星环厚度。
 */
public final class SaturnRingTerrainSampler {
    // 将平滑半径设为 5，并搭配更大的步长以实现超远距离的缓坡
    private static final int RADIUS = 5;
    private static final float[] BIOME_WEIGHT_TABLE = net.minecraft.Util.make(new float[(RADIUS * 2 + 1) * (RADIUS * 2 + 1)], array -> {
        for (int x = -RADIUS; x <= RADIUS; ++x) {
            for (int z = -RADIUS; z <= RADIUS; ++z) {
                // 使用类似高斯模糊的曲线让权重极其平滑地衰减
                float distanceSq = (float)(x * x + z * z);
                float maxDistanceSq = (float)(RADIUS * RADIUS);
                // 如果超出半径，权重圆润且平滑地趋于零
                float weight = (float) Math.exp(-distanceSq / (maxDistanceSq * 0.4f));
                array[(x + RADIUS) + (z + RADIUS) * (RADIUS * 2 + 1)] = weight;
            }
        }
    });

    public record SampledTerrain(double thickness, double roughness) {}

    /**
     * 计算该坐标的平滑期望厚度与地形起伏（崎岖度）。
     */
    public static SampledTerrain sampleTerrain(int x, int z, Climate.Sampler sampler, BiomeSource biomeSource) {
        // 先获取目标中心点的群系信息
        Holder<Biome> centerBiome = biomeSource.getNoiseBiome(x >> 2, 0, z >> 2, sampler);
        SaturnRingBiomeMetrics.TerrainMetrics centerMetrics = SaturnRingBiomeMetrics.getMetrics(centerBiome);

        float totalThickness = 0.0F;
        float totalRoughness = 0.0F;
        float totalWeight = 0.0F;

        // 采样周围区域
        for (int xOffset = -RADIUS; xOffset <= RADIUS; ++xOffset) {
            for (int zOffset = -RADIUS; zOffset <= RADIUS; ++zOffset) {
                // 步长设为 6，加上半径 5，总计会在周围跨越半径跨度 30 的 Noise区（=120满格方块），两边生物群系的过渡带宽达 240 格方块
                Holder<Biome> sampledBiome = biomeSource.getNoiseBiome((x >> 2) + xOffset * 6, 0, (z >> 2) + zOffset * 6, sampler);
                SaturnRingBiomeMetrics.TerrainMetrics sampledMetrics = SaturnRingBiomeMetrics.getMetrics(sampledBiome);

                float currentThickness = sampledMetrics.thickness;
                float currentRoughness = sampledMetrics.roughness;
                float weight = BIOME_WEIGHT_TABLE[(xOffset + RADIUS) + (zOffset + RADIUS) * (RADIUS * 2 + 1)];

                // 如果周围群系厚度不同，使用中心群系的权重系数作缓和插值
                if (currentThickness != centerMetrics.thickness) {
                    currentThickness = Mth.lerp(centerMetrics.weightModifier, currentThickness, centerMetrics.thickness);
                }
                if (currentRoughness != centerMetrics.roughness) {
                    currentRoughness = Mth.lerp(centerMetrics.weightModifier, currentRoughness, centerMetrics.roughness);
                }

                totalThickness += currentThickness * weight;
                totalRoughness += currentRoughness * weight;
                totalWeight += weight;
            }
        }

        // 返回归一化的平滑厚度与平滑崎岖度
        return new SampledTerrain(totalThickness / totalWeight, totalRoughness / totalWeight);
    }
}
