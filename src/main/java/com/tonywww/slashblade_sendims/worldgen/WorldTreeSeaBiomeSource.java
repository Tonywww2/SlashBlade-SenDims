package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import java.util.stream.Stream;

public final class WorldTreeSeaBiomeSource extends BiomeSource {
    public static final Codec<WorldTreeSeaBiomeSource> CODEC=RecordCodecBuilder.create(i->i.group(
        Biome.CODEC.fieldOf("roots").forGetter(s->s.roots),
        Biome.CODEC.fieldOf("shallows").forGetter(s->s.shallows),
        Biome.CODEC.fieldOf("depths").forGetter(s->s.depths),
        Codec.LONG.optionalFieldOf("layout_seed",20261004L).forGetter(s->s.field.seed),
        Codec.intRange(16,96).optionalFieldOf("sea_level",64).forGetter(s->s.field.seaLevel),
        Codec.intRange(280,600).optionalFieldOf("tree_spacing",360).forGetter(s->s.field.spacing),
        Codec.intRange(120,400).optionalFieldOf("crown_radius",289).forGetter(s->s.field.crownRadius)
    ).apply(i,WorldTreeSeaBiomeSource::new));
    private final Holder<Biome> roots,shallows,depths;
    public final WorldTreeField field;
    public WorldTreeSeaBiomeSource(Holder<Biome> roots,Holder<Biome> shallows,Holder<Biome> depths,long seed,int sea,int spacing,int crown){
        this.roots=roots;this.shallows=shallows;this.depths=depths;field=new WorldTreeField(seed,sea,spacing,crown);
    }
    @Override protected Codec<? extends BiomeSource> codec(){return CODEC;}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.of(roots,shallows,depths);}
    @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){
        // Use the identical terrain classifier at quart coordinates, at every altitude.
        int floor=field.floorAt(x<<2,z<<2);
        return floor>=field.seaLevel ? roots : floor>=field.seaLevel-11 ? shallows : depths;
    }
}
