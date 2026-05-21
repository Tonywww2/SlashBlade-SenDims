package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 负责土星环方块填充、消散区空洞计算与物理厚度边界管理的地形生成器。
 */
public class SaturnRingChunkGenerator extends ChunkGenerator {

    public static final Codec<SaturnRingChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SaturnRingBiomeSource.CODEC.fieldOf("biome_source").forGetter(cg -> (SaturnRingBiomeSource) cg.getBiomeSource())
    ).apply(instance, SaturnRingChunkGenerator::new));

    // 环的基础 Y 高度（在这个高度上向上下生成带状结构）
    private static final int RING_Y_CENTER = 64;

    private final SimplexNoise porosityNoise = new SimplexNoise(new net.minecraft.world.level.levelgen.LegacyRandomSource(114514L));
    // 添加专门的 2D 柏林高度噪声，用于主世界般的丘陵/平原起伏生成
    private final SimplexNoise heightNoise = new SimplexNoise(new net.minecraft.world.level.levelgen.LegacyRandomSource(223344L));

    public SaturnRingChunkGenerator(SaturnRingBiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(net.minecraft.server.level.WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, net.minecraft.world.level.StructureManager structureManager, ChunkAccess chunk, net.minecraft.world.level.levelgen.GenerationStep.Carving step) {
        // 不需要原版的洞穴生成器
    }

    @Override
    public void buildSurface(net.minecraft.server.level.WorldGenRegion region, net.minecraft.world.level.StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // 简化的表面生成逻辑
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = chunk.getPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getMinBlockX() + x;
                int worldZ = chunkPos.getMinBlockZ() + z;

                Holder<Biome> biomeHolder = this.biomeSource.getNoiseBiome(worldX >> 2, 0, worldZ >> 2, randomState.sampler());

                // 获取最高方块和最低方块的 Y (即环的顶底)
                int maxY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);

                // 为了应用不同群系的表面材质：
                BlockState surfaceState = Blocks.SANDSTONE.defaultBlockState();
                if (biomeHolder.is(SaturnRingBiomes.HIGH_DENSITY_RING)) {
                    surfaceState = SaturnRingBlocks.HIGH_DENSITY_SURFACE;
                } else if (biomeHolder.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
                    surfaceState = SaturnRingBlocks.TRANSITION_WALL_SURFACE;
                } else if (biomeHolder.is(SaturnRingBiomes.INNER_SPARSE_RING) || biomeHolder.is(SaturnRingBiomes.OUTER_SPARSE_RING)) {
                    surfaceState = SaturnRingBlocks.SPARSE_RING_CORE;
                } else if (biomeHolder.is(SaturnRingBiomes.VOID_RING) || biomeHolder.is(SaturnRingBiomes.INNER_FADING_RING)) {
                    surfaceState = Blocks.END_STONE.defaultBlockState();
                }

                if (maxY > chunk.getMinBuildHeight()) {
                    pos.set(x, maxY, z);
                    BlockState currentBlock = chunk.getBlockState(pos);
                    // 只有在我们用占位岩石填充的地方才替换表面
                    if (!currentBlock.isAir()) {
                        chunk.setBlockState(pos, surfaceState, false);
                    }
                }
            }
        }
    }

    @Override
    public void spawnOriginalMobs(net.minecraft.server.level.WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 128;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, net.minecraft.world.level.StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;

                Holder<Biome> biomeHolder = this.biomeSource.getNoiseBiome(worldX >> 2, 0, worldZ >> 2, randomState.sampler());

                double expectedThickness;
                double expectedRoughness;
                double topOffset;
                double bottomOffset;

                // 如果是过渡墙，直接阻断平滑体系，使用独立的巨高生成限制，靠内部噪声来切削出石头墙感
                if (biomeHolder.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
                    expectedThickness = 120.0; // 极高的高度限制 (接近整个128允许高度区)
                    expectedRoughness = 0.0;   // 不使用山丘系统
                    topOffset = 0;
                    bottomOffset = 0;
                } else {
                    // 1. 获取平滑后的预期环厚度与坎坷起伏程度
                    SaturnRingTerrainSampler.SampledTerrain terrain = SaturnRingTerrainSampler.sampleTerrain(worldX, worldZ, randomState.sampler(), this.biomeSource);
                    expectedThickness = terrain.thickness();
                    expectedRoughness = terrain.roughness();
                    // 计算顶面高度起伏量（丘陵/山脉的振幅），缩放系数为0.005表现极其平缓开阔的山坡
                    topOffset = heightNoise.getValue(worldX * 0.005, worldZ * 0.005) * expectedRoughness;
                    // 添加底部不规则性，取个偏移量让它和顶部的山丘不同步
                    bottomOffset = heightNoise.getValue(worldX * 0.005 + 1000, worldZ * 0.005 + 1000) * expectedRoughness;
                }

                if (expectedThickness <= 0.5) {
                    continue; // 虚空区域，直接跳过生成
                }

                // 半厚度
                int halfThickness = (int) Math.round(expectedThickness / 2.0);

                int topY = RING_Y_CENTER + halfThickness + (int) Math.round(topOffset);
                int bottomY = RING_Y_CENTER - halfThickness + (int) Math.round(bottomOffset);

                for (int y = bottomY; y <= topY; y++) {
                    pos.set(localX, y, localZ);

                    // 2. 空洞与消散逻辑 (Porosity & Fading)
                    if (isPorous(worldX, y, worldZ, biomeHolder)) {
                        continue; // 被 3D 噪声挖空的地方保留空气
                    }

                    // 3. 基础填充（核心方块）
                    BlockState coreState = SaturnRingBlocks.BASE_RING_CORE;
                    if(biomeHolder.is(SaturnRingBiomes.HIGH_DENSITY_RING)) {
                        coreState = SaturnRingBlocks.HIGH_DENSITY_CORE;
                    } else if (biomeHolder.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
                        coreState = SaturnRingBlocks.TRANSITION_WALL_CORE;
                    } else if (biomeHolder.is(SaturnRingBiomes.INNER_SPARSE_RING) || biomeHolder.is(SaturnRingBiomes.OUTER_SPARSE_RING) || biomeHolder.is(SaturnRingBiomes.INNER_FADING_RING)) {
                        coreState = SaturnRingBlocks.SPARSE_RING_CORE;
                    }

                    chunk.setBlockState(pos, coreState, false);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * 判断特定的 xyz 坐标是否因为空洞噪声而成为空气。
     * 可以模拟陨石坑、稀疏颗粒或者消散区的沙化。
     */
    private boolean isPorous(int x, int y, int z, Holder<Biome> biome) {
        // 在内部衰减区域 (-6000 到 -10000)，Z 越靠近 -10000，变成空气的概率越高。
        if (biome.is(SaturnRingBiomes.INNER_FADING_RING)) {
            // Z 在 -6000 到 -10000 之间。将其映射到 0.0 到 1.0 的衰减系数
            double fadeRatio = (Math.abs(z) - 6000.0) / 4000.0;
            fadeRatio = Math.max(0, Math.min(1.0, fadeRatio));

            // 使用较高频3D噪音计算阈值
            double noiseVal = porosityNoise.getValue(x * 0.03, y * 0.08, z * 0.03);

            // fadeRatio=0(靠近-6000) 时, 需要噪音 <-0.8 才变空气(很少孔)
            // fadeRatio=1(靠近-10000) 时, 需要噪音 < 0.8 就变空气(全是孔，最后彻底没有)
            double threshold = -0.8 + (1.6 * fadeRatio);
            return noiseVal < threshold;

        } else if (biome.is(SaturnRingBiomes.INNER_SPARSE_RING)) {
            // 稀疏区偶尔有固定频率的小型孔洞 (坑洼)
            double noiseVal = porosityNoise.getValue(x * 0.05, y * 0.1, z * 0.05);
            return noiseVal < -0.4;
        } else if (biome.is(SaturnRingBiomes.HIGH_DENSITY_RING)) {
            // 高密度区偶尔有大裂缝（X轴长裂隙分布）
            // 这是利用拉伸 Z 轴噪音的非等比采样形成裂谷的效果
            double crackNoise = porosityNoise.getValue(x * 0.01, y * 0.01, z * 0.05);
            return crackNoise > 0.6; // 产生极少但非常深刻的空洞柱
        } else if (biome.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            // 过渡墙是一片小行星碎石带。我们用三维高频噪声切碎它，做成无数的碎片岩石浮空集合体。
            double wallNoise = porosityNoise.getValue(x * 0.15, y * 0.15, z * 0.15);
            // 只有 15% 的几率留下实体方块（noise > 0.7），其余全是空气。极度碎片化的太空垃圾感！
            return wallNoise < 0.7;
        }

        // 基础区基本完整，没有额外空隙
        return false;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor heightAccessor, RandomState randomState) {
        Holder<Biome> biomeHolder = this.biomeSource.getNoiseBiome(x >> 2, 0, z >> 2, randomState.sampler());
        if (biomeHolder.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            return RING_Y_CENTER + 60; // 对应 120 的厚度半值
        }

        SaturnRingTerrainSampler.SampledTerrain terrain = SaturnRingTerrainSampler.sampleTerrain(x, z, randomState.sampler(), this.biomeSource);
        double expectedThickness = terrain.thickness();
        if (expectedThickness <= 0.5) return getMinY();

        // 降低高度采集中的频率匹配表面生成
        double topOffset = heightNoise.getValue(x * 0.005, z * 0.005) * terrain.roughness();
        return RING_Y_CENTER + (int) Math.round(expectedThickness / 2.0) + (int) Math.round(topOffset);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        // 这是原版在寻路和结构放置前探测地形所需的方法。可以返回简单的包含方块的柱子。
        int topY = getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState);
        
        Holder<Biome> biomeHolder = this.biomeSource.getNoiseBiome(x >> 2, 0, z >> 2, randomState.sampler());
        double expectedThicknessBase;
        int bottomY;
        if (biomeHolder.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            expectedThicknessBase = 120.0;
            bottomY = RING_Y_CENTER - 60;
        } else {
            SaturnRingTerrainSampler.SampledTerrain terrainBase = SaturnRingTerrainSampler.sampleTerrain(x, z, randomState.sampler(), this.biomeSource);
            expectedThicknessBase = terrainBase.thickness();
            double bottomOffset = heightNoise.getValue(x * 0.005 + 1000, z * 0.005 + 1000) * terrainBase.roughness();
            bottomY = RING_Y_CENTER - (int) Math.round(expectedThicknessBase / 2.0) + (int) Math.round(bottomOffset);
        }

        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        for(int i = 0; i < states.length; i++){
            int actualY = heightAccessor.getMinBuildHeight() + i;
            if(actualY >= bottomY && actualY <= topY && expectedThicknessBase > 0.5){
                states[i] = SaturnRingBlocks.BASE_RING_CORE;
            } else {
                states[i] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {
    }
}
