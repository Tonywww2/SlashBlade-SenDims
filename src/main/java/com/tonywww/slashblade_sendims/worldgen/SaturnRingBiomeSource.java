package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class SaturnRingBiomeSource extends BiomeSource {
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

    private final SimplexNoise boundaryWarpNoise = new SimplexNoise(new LegacyRandomSource(667408L));

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
        int blockX = x << 2;
        int blockZ = z << 2;
        double warpedZ = blockZ + sampleBoundaryWarp(blockX, blockZ);

        double[] boundaries = {-6000, -2000, 2000, 6000, 10000};
        for (double boundary : boundaries) {
            if (Math.abs(warpedZ - boundary) <= 32) {
                return transitionWallRing;
            }
        }

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
            return baseRing;
        } else if (warpedZ <= 14000) {
            return outerSparseRing;
        }
        return voidRing;
    }

    private double sampleBoundaryWarp(int blockX, int blockZ) {
        double ribbonWave = Math.sin(blockX * 0.0005) * 130.0;
        double macroWarp = boundaryWarpNoise.getValue(blockX * 0.0007, blockZ * 0.0007) * 260.0;
        double detailWarp = boundaryWarpNoise.getValue(blockX * 0.0020 + 300.0, blockZ * 0.0020 - 300.0) * 60.0;
        return ribbonWave + macroWarp + detailWarp;
    }
}
