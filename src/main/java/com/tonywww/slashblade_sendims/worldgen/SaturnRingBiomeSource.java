package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

/**
 * 沿 Z 轴（半径）进行群系分布的一维群系生成器，加入了轻微的 X 轴扰动以模糊分界线。
 */
public class SaturnRingBiomeSource extends BiomeSource {

    // 使用资源提供器获取群系（适用于1.20版本）
    public static final Codec<SaturnRingBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.CODEC.fieldOf("void_ring").forGetter(source -> source.voidRing),
            Biome.CODEC.fieldOf("inner_fading_ring").forGetter(source -> source.innerFadingRing),
            Biome.CODEC.fieldOf("inner_sparse_ring").forGetter(source -> source.innerSparseRing),
            Biome.CODEC.fieldOf("base_ring").forGetter(source -> source.baseRing),
            Biome.CODEC.fieldOf("high_density_ring").forGetter(source -> source.highDensityRing),
            Biome.CODEC.fieldOf("outer_sparse_ring").forGetter(source -> source.outerSparseRing),
            Biome.CODEC.fieldOf("transition_wall_ring").forGetter(source -> source.transitionWallRing)
    ).apply(instance, SaturnRingBiomeSource::new));

    private final Holder<Biome> voidRing;
    private final Holder<Biome> innerFadingRing;
    private final Holder<Biome> innerSparseRing;
    private final Holder<Biome> baseRing;
    private final Holder<Biome> highDensityRing;
    private final Holder<Biome> outerSparseRing;
    private final Holder<Biome> transitionWallRing;

    public SaturnRingBiomeSource(Holder<Biome> voidRing, Holder<Biome> innerFadingRing,
                                 Holder<Biome> innerSparseRing, Holder<Biome> baseRing,
                                 Holder<Biome> highDensityRing, Holder<Biome> outerSparseRing,
                                 Holder<Biome> transitionWallRing) {
        this.voidRing = voidRing;
        this.innerFadingRing = innerFadingRing;
        this.innerSparseRing = innerSparseRing;
        this.baseRing = baseRing;
        this.highDensityRing = highDensityRing;
        this.outerSparseRing = outerSparseRing;
        this.transitionWallRing = transitionWallRing;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(voidRing, innerFadingRing, innerSparseRing, baseRing, highDensityRing, outerSparseRing, transitionWallRing);
    }

    @Override
    public @NotNull Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.@NotNull Sampler sampler) {
        // x,y,z 为 noise 坐标，实际方块坐标需要乘 4
        int blockX = x << 2;
        int blockZ = z << 2;

        // 简单的单弧线扰动（超低频率、较缓的大型正弦波，形似巨大的单一弧度）
        double warp = Math.sin(blockX * 0.0005) * 150.0;
        double warpedZ = blockZ + warp;

        // 小行星带墙界线侦测：在原有各个区的边界附加宽为 48 (半径 24)的墙群系
        double[] boundaries = {-6000, -2000, 2000, 6000, 10000};
        for (double boundary : boundaries) {
            if (Math.abs(warpedZ - boundary) <= 24) {
                return transitionWallRing;
            }
        }

        // 根据 Z 坐标对应不同的区域
        if (warpedZ < -10000) {
            return voidRing;
        } else if (warpedZ < -6000) {
            return innerFadingRing;
        } else if (warpedZ < -2000) {
            return innerSparseRing;
        } else if (warpedZ <= 2000) {
            return baseRing;
        } else if (warpedZ <= 6000) {
            return highDensityRing;
        } else if (warpedZ <= 10000) {
            return baseRing; // 重复的基础区用于过渡
        } else if (warpedZ <= 14000) {
            return outerSparseRing;
        } else {
            return voidRing;
        }
    }
}
