package com.tonywww.slashblade_sendims.worldgen;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 集中管理土星环维度中使用的方块资源
 * 方便后续进行方块的全局替换
 */
public class SaturnRingBlocks {
    // 基础环方块（默认使用砂岩，后续可替换为自定义方块）
    public static BlockState BASE_RING_SURFACE = Blocks.SANDSTONE.defaultBlockState();
    public static BlockState BASE_RING_SUBSURFACE = Blocks.SANDSTONE.defaultBlockState();
    public static BlockState BASE_RING_CORE = Blocks.SMOOTH_SANDSTONE.defaultBlockState();

    // 高密度区方块（使用深黑色调方块）
    public static BlockState HIGH_DENSITY_SURFACE = Blocks.DEEPSLATE.defaultBlockState();
    public static BlockState HIGH_DENSITY_CORE = Blocks.DEEPSLATE.defaultBlockState();

    // 过渡墙/小行星带（碎裂的深色岩石和凝灰岩）
    public static BlockState TRANSITION_WALL_SURFACE = Blocks.COBBLED_DEEPSLATE.defaultBlockState();
    public static BlockState TRANSITION_WALL_CORE = Blocks.TUFF.defaultBlockState();

    // 稀疏区方块（偶尔会有风化碎岩）
    public static BlockState SPARSE_RING_CORE = Blocks.END_STONE.defaultBlockState();
}
