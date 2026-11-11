package com.tonywww.slashblade_sendims.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 深境1层的岩柱直接用目标方块生成（2026-11-11）。
 * <p>
 * 原数据包先用原版 large_dripstone / basalt_pillar 生成滴水石块、玄武岩柱，再用每区块数千次的 netherrack_replace_blobs
 * 把它们换成深板岩（7 个群系，30×180 次）和方解石（深岩洞窟，30×150 次）。替换每次都要从随机高度向下扫描整列、
 * 再遍历半径 12 的菱形范围，是深境1层区块生成最重的部分。这里让两种原版地物在对应位置直接放置目标方块，
 * 数据包里的两个替换地物随之移除，生成结果相同。
 */
public final class DeepRealmPillarBlocks {
    public static final ResourceKey<Level> DEEP_REALM_1 =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sdbf", "deep_realm_level_1"));
    public static final ResourceKey<Biome> DEEPROCK_CAVERNS =
            ResourceKey.create(Registries.BIOME, new ResourceLocation("sdbf", "deep_realm_1/deeprock_caverns"));

    private static final ThreadLocal<Block> LARGE_DRIPSTONE_BLOCK = new ThreadLocal<>();
    private static final ThreadLocal<Block> BASALT_PILLAR_BLOCK = new ThreadLocal<>();

    private DeepRealmPillarBlocks() {}

    /** 深境1层的大型滴水石（generic/big_pillar、small_pillar）直接用深板岩。 */
    public static void beginLargeDripstone(WorldGenLevel level) {
        LARGE_DRIPSTONE_BLOCK.set(DEEP_REALM_1.equals(level.getLevel().dimension()) ? Blocks.DEEPSLATE : null);
    }

    public static void endLargeDripstone() {
        LARGE_DRIPSTONE_BLOCK.remove();
    }

    public static Block largeDripstoneBlock() {
        Block block = LARGE_DRIPSTONE_BLOCK.get();
        return block != null ? block : Blocks.DRIPSTONE_BLOCK;
    }

    /** 起点在深岩洞窟的玄武岩柱直接用方解石；熔岩洞窟等其他群系保持玄武岩。 */
    public static void beginBasaltPillar(WorldGenLevel level, BlockPos origin) {
        BASALT_PILLAR_BLOCK.set(level.getBiome(origin).is(DEEPROCK_CAVERNS) ? Blocks.CALCITE : null);
    }

    public static void endBasaltPillar() {
        BASALT_PILLAR_BLOCK.remove();
    }

    public static Block basaltPillarBlock() {
        Block block = BASALT_PILLAR_BLOCK.get();
        return block != null ? block : Blocks.BASALT;
    }
}
