package com.tonywww.slashblade_sendims.worldgen;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

/**
 * 注册和调用土星环维度的特定生物群系
 */
public class SaturnRingBiomes {
    // 完整的虚空群系（环的更外围或完全消散区）
    public static final ResourceKey<Biome> VOID_RING = createKey("void_ring");

    // 内环消散区：Z: -10000 到 -6000 (密度线性衰减)
    public static final ResourceKey<Biome> INNER_FADING_RING = createKey("inner_fading_ring");

    // 内环稀疏区：Z: -6000 到 -2000 (厚度大约4，多空洞)
    public static final ResourceKey<Biome> INNER_SPARSE_RING = createKey("inner_sparse_ring");

    // 基础环区（内）：Z: -2000 到 2000 或者 Z: 6000 到 10000 (厚度10，砂岩主导)
    public static final ResourceKey<Biome> BASE_RING = createKey("base_ring");

    // 高密度环区：Z: 2000 到 6000 (陡峭，大裂缝，深色调)
    public static final ResourceKey<Biome> HIGH_DENSITY_RING = createKey("high_density_ring");

    // 过渡墙区：位于所有基础环交界线上，宽度较窄的小行星带阻隔墙
    public static final ResourceKey<Biome> TRANSITION_WALL_RING = createKey("transition_wall_ring");

    // 外环稀疏区：Z: 10000+ (过渡回外围稀疏或虚空)
    public static final ResourceKey<Biome> OUTER_SPARSE_RING = createKey("outer_sparse_ring");

    private static ResourceKey<Biome> createKey(String name) {
        return ResourceKey.create(Registries.BIOME, SenDims.prefix(name));
    }
}
