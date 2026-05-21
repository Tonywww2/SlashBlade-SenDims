package com.tonywww.slashblade_sendims.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * 记录土星环每个生物群系对应的【预期带状地形厚度】以及【高度权重系数】。
 * 类似于 Bumblezone 的 BzBiomeHeightRegistry
 */
public class SaturnRingBiomeMetrics {

    public static class TerrainMetrics {
        public final float thickness;     // 土星环在该群系的厚度（Y轴范围）
        public final float weightModifier;// 过渡到其他高度时的插值权重
        public final float roughness;     // 地形的起伏和崎岖程度

        public TerrainMetrics(float thickness, float weightModifier, float roughness) {
            this.thickness = thickness;
            this.weightModifier = weightModifier;
            this.roughness = roughness;
        }
    }

    private static final Map<ResourceKey<Biome>, TerrainMetrics> METRICS_MAP = new HashMap<>();

    static {
        // 虚空区域完全没有厚度
        METRICS_MAP.put(SaturnRingBiomes.VOID_RING, new TerrainMetrics(0.0f, 1.0f, 0.0f));
        // 消散区厚度从4逐渐衰亡
        METRICS_MAP.put(SaturnRingBiomes.INNER_FADING_RING, new TerrainMetrics(2.0f, 0.8f, 1.0f));
        // 稀疏区厚度约为 4
        METRICS_MAP.put(SaturnRingBiomes.INNER_SPARSE_RING, new TerrainMetrics(4.0f, 1.0f, 2.0f));
        // 基础区厚度约为 10，伴随一般平原的起伏
        METRICS_MAP.put(SaturnRingBiomes.BASE_RING, new TerrainMetrics(10.0f, 1.0f, 3.5f));
        // 高密度区最厚，且更易产生高低起伏(比较险峻)
        METRICS_MAP.put(SaturnRingBiomes.HIGH_DENSITY_RING, new TerrainMetrics(16.0f, 1.5f, 7.0f));
        // 过渡墙（小行星带碎石墙），数值可设置为占满剩余空间
        METRICS_MAP.put(SaturnRingBiomes.TRANSITION_WALL_RING, new TerrainMetrics(100.0f, 0.0f, 0.0f)); // 我们在chunk generator中会直接跳过这部分的平滑以维持悬浮小行星的随机散落感
        // 外围稀疏区
        METRICS_MAP.put(SaturnRingBiomes.OUTER_SPARSE_RING, new TerrainMetrics(2.0f, 0.8f, 1.5f));
    }

    public static TerrainMetrics getMetrics(Holder<Biome> biomeHolder) {
        return biomeHolder.unwrapKey().map(METRICS_MAP::get)
                .orElse(new TerrainMetrics(0.0f, 1.0f, 0.0f)); // 默认找不到则当作虚空
    }
}
