package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public final class RemappedEndBiomeSource extends BiomeSource {
    public static final Codec<RemappedEndBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.CODEC.fieldOf("end").forGetter(source -> source.end),
            Biome.CODEC.fieldOf("highlands").forGetter(source -> source.highlands),
            Biome.CODEC.fieldOf("midlands").forGetter(source -> source.midlands),
            Biome.CODEC.fieldOf("islands").forGetter(source -> source.islands),
            Biome.CODEC.fieldOf("barrens").forGetter(source -> source.barrens)
    ).apply(instance, RemappedEndBiomeSource::new));

    private final Holder<Biome> end;
    private final Holder<Biome> highlands;
    private final Holder<Biome> midlands;
    private final Holder<Biome> islands;
    private final Holder<Biome> barrens;

    private RemappedEndBiomeSource(
            Holder<Biome> end,
            Holder<Biome> highlands,
            Holder<Biome> midlands,
            Holder<Biome> islands,
            Holder<Biome> barrens
    ) {
        this.end = end;
        this.highlands = highlands;
        this.midlands = midlands;
        this.islands = islands;
        this.barrens = barrens;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(end, highlands, midlands, islands, barrens);
    }

    public Holder<Biome> getEndBiome() {
        return this.end;
    }

    @Override
    public @NotNull Holder<Biome> getNoiseBiome(
            int quartX,
            int quartY,
            int quartZ,
            Climate.@NotNull Sampler sampler
    ) {
        int sectionX = SectionPos.blockToSectionCoord(QuartPos.toBlock(quartX));
        int sectionZ = SectionPos.blockToSectionCoord(QuartPos.toBlock(quartZ));

        if ((long) sectionX * sectionX + (long) sectionZ * sectionZ <= 4096L) {
            return end;
        }

        int sampleX = (sectionX * 2 + 1) * 8;
        int sampleZ = (sectionZ * 2 + 1) * 8;
        double erosion = sampler.erosion().compute(
                new DensityFunction.SinglePointContext(sampleX, QuartPos.toBlock(quartY), sampleZ)
        );

        if (erosion > 0.25D) {
            return highlands;
        }
        if (erosion >= -0.0625D) {
            return midlands;
        }
        if (erosion < -0.21875D) {
            return islands;
        }
        return barrens;
    }
}