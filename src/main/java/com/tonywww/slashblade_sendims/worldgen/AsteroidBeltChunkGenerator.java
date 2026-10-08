package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import java.util.*;
import java.util.concurrent.*;

public final class AsteroidBeltChunkGenerator extends ChunkGenerator {
    public record ScatteredOre(BlockState state,int weight){
        public static final Codec<ScatteredOre> CODEC=RecordCodecBuilder.create(i->i.group(
            BlockState.CODEC.fieldOf("state").forGetter(ScatteredOre::state),
            Codec.intRange(1,256).fieldOf("weight").forGetter(ScatteredOre::weight)
        ).apply(i,ScatteredOre::new));
    }
    public record OreProfile(String id,BlockState core,BlockState inner,BlockState alternate,BlockState middle,BlockState outer){
        public static final Codec<OreProfile> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.STRING.fieldOf("id").forGetter(OreProfile::id),
            BlockState.CODEC.fieldOf("core").forGetter(OreProfile::core),
            BlockState.CODEC.fieldOf("inner").forGetter(OreProfile::inner),
            BlockState.CODEC.fieldOf("alternate").forGetter(OreProfile::alternate),
            BlockState.CODEC.fieldOf("middle").forGetter(OreProfile::middle),
            BlockState.CODEC.fieldOf("outer").forGetter(OreProfile::outer)
        ).apply(i,OreProfile::new));
    }
    public static final Codec<AsteroidBeltChunkGenerator> CODEC=RecordCodecBuilder.create(i->i.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(g->g.getBiomeSource()),
        Codec.LONG.optionalFieldOf("layout_seed",20261006L).forGetter(g->g.layoutSeed),
        BlockState.CODEC.listOf().fieldOf("rocks").forGetter(g->g.rocks),
        BlockState.CODEC.listOf().fieldOf("ores").forGetter(g->g.ores),
        OreProfile.CODEC.listOf().optionalFieldOf("ore_profiles",List.of()).forGetter(g->g.oreProfiles),
        ScatteredOre.CODEC.listOf().optionalFieldOf("scattered_ores",List.of()).forGetter(g->g.scatteredOres),
        Codec.doubleRange(0,1).optionalFieldOf("scattered_ore_chance",.01).forGetter(g->g.scatteredOreChance)
    ).apply(i,AsteroidBeltChunkGenerator::new));
    private final long layoutSeed;
    private final List<BlockState> rocks,ores;
    private final List<OreProfile> oreProfiles;
    private final List<ScatteredOre> scatteredOres;
    private final List<BlockState> scatteredPalette;
    private final double scatteredOreChance;
    private volatile AsteroidBeltField field;
    private final Map<Long,AsteroidBeltGarden.Plan> gardens=Collections.synchronizedMap(new LinkedHashMap<>(64,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Long,AsteroidBeltGarden.Plan> e){return size()>64;}
    });
    public AsteroidBeltChunkGenerator(BiomeSource source,long seed,List<BlockState> rocks,List<BlockState> ores,List<OreProfile> oreProfiles,List<ScatteredOre> scatteredOres,double scatteredOreChance){
        super(source);layoutSeed=seed;this.rocks=List.copyOf(rocks);this.ores=List.copyOf(ores);field=new AsteroidBeltField(seed);
        this.oreProfiles=List.copyOf(oreProfiles);
        this.scatteredOres=List.copyOf(scatteredOres);this.scatteredOreChance=scatteredOreChance;
        List<BlockState> palette=new ArrayList<>();
        for(var ore:scatteredOres){
            if(ore.weight()<1||ore.weight()>256||ore.state().isAir()||!ore.state().getFluidState().isEmpty())throw new IllegalArgumentException("Invalid scattered ore entry");
            palette.addAll(Collections.nCopies(ore.weight(),ore.state()));
        }
        if(palette.size()>65536||scatteredOreChance<0||scatteredOreChance>1)throw new IllegalArgumentException("Invalid scattered ore palette/chance");
        scatteredPalette=List.copyOf(palette);
        if(rocks.size()!=3||ores.isEmpty())throw new IllegalArgumentException("Asteroid belt needs three rock states and a nonempty ore palette");
        if(!oreProfiles.isEmpty()&&oreProfiles.size()!=ores.size())throw new IllegalArgumentException("Ore profiles must match the weighted ore palette");
    }
    public List<OreProfile> oreProfiles(){return oreProfiles;}
    public List<ScatteredOre> scatteredOres(){return scatteredOres;}
    public double scatteredOreChance(){return scatteredOreChance;}
    public OreProfile oreProfile(AsteroidBeltField.Body b){return oreProfiles.isEmpty()?new OreProfile("fallback",ores.get(b.ore%ores.size()),rocks.get(2),rocks.get(0),rocks.get(0),rocks.get(2)):oreProfiles.get(b.ore%oreProfiles.size());}
    @Override public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> lookup,RandomState random,long seed){
        field=new AsteroidBeltField(seed^layoutSeed);gardens.clear();return super.createState(lookup,random,seed);
    }
    public AsteroidBeltField field(){return field;}
    public AsteroidBeltGarden.Plan garden(AsteroidBeltField.Body b){
        // Decoration positions belong to a spatial cell even when a shape seed is repeated.
        long key=AsteroidBeltField.key((int)Math.floor(b.x/AsteroidBeltField.CELL),(int)Math.floor(b.z/AsteroidBeltField.CELL));
        var old=gardens.get(key);if(old!=null)return old;var plan=AsteroidBeltGarden.create(b);gardens.put(key,plan);return plan;
    }
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public int getGenDepth(){return AsteroidBeltField.HEIGHT;}
    @Override public int getMinY(){return 0;}
    @Override public int getSeaLevel(){return 0;}
    @Override public void applyCarvers(WorldGenRegion region,long seed,RandomState state,BiomeManager manager,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step){}
    @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState state,ChunkAccess chunk){}
    @Override public void spawnOriginalMobs(WorldGenRegion region){}
    public BlockState state(AsteroidBeltField.Material m,AsteroidBeltField.Body b){
        return switch(m){
            case AIR->Blocks.AIR.defaultBlockState();case ROCK->rocks.get(0);case CARBON->rocks.get(1);case SILICA->rocks.get(2);
            case ORE->b.type==AsteroidBeltField.Type.MINERAL?oreProfile(b).core():ores.get(b.ore%ores.size());
            case MINERAL_INNER->oreProfile(b).inner();case MINERAL_ALTERNATE->oreProfile(b).alternate();case MINERAL_MIDDLE->oreProfile(b).middle();case MINERAL_OUTER->oreProfile(b).outer();
            case PACKED_ICE->Blocks.PACKED_ICE.defaultBlockState();case BLUE_ICE->Blocks.BLUE_ICE.defaultBlockState();case ICE->Blocks.ICE.defaultBlockState();
            case DIRT->Blocks.DIRT.defaultBlockState();case GRASS->Blocks.GRASS_BLOCK.defaultBlockState();case WATER->Blocks.WATER.defaultBlockState();case GLOWSTONE->Blocks.GLOWSTONE.defaultBlockState();
        };
    }
    public BlockState[] column(int x,int z,List<AsteroidBeltField.Body> candidates,boolean decorate){
        BlockState[] out=new BlockState[getGenDepth()];Arrays.fill(out,Blocks.AIR.defaultBlockState());
        List<int[]> reserved=new ArrayList<>(2);
        for(var b:candidates)if(b.type==AsteroidBeltField.Type.GARDEN){var interval=b.gardenInterval(x,z);if(interval!=null)reserved.add(interval);}
        for(var b:candidates){
            if(!b.intersects(x,z,x,z))continue;
            for(int y=b.minY();y<=b.maxY();y++){
                boolean skip=false;if(b.type!=AsteroidBeltField.Type.GARDEN)for(var interval:reserved)if(y>=interval[0]&&y<=interval[1]){skip=true;break;}
                if(skip)continue;var m=b.sample(x,y,z);if(m!=AsteroidBeltField.Material.AIR)out[y]=scatteredState(m,b,x,y,z);
            }
            if(decorate&&b.type==AsteroidBeltField.Type.GARDEN)for(var v:garden(b).chunks().getOrDefault(AsteroidBeltField.key(x>>4,z>>4),List.of()))
                if(v.pos().getX()==x&&v.pos().getZ()==z&&out[v.pos().getY()].isAir())out[v.pos().getY()]=v.state();
        }
        return out;
    }
    private BlockState scatteredState(AsteroidBeltField.Material material,AsteroidBeltField.Body body,int x,int y,int z){
        BlockState original=state(material,body);
        if(scatteredPalette.isEmpty()||scatteredOreChance==0)return original;
        // Replace inert hull/rock material only. Existing ore cores and the garden's
        // living ground, water and lights keep their original states and support.
        boolean host=switch(material){
            case ROCK,CARBON,SILICA,PACKED_ICE,BLUE_ICE,ICE,MINERAL_INNER,MINERAL_ALTERNATE,MINERAL_MIDDLE,MINERAL_OUTER->true;
            default->false;
        };
        if(!host)return original;
        // One spatial hash, independent of height and body size/type: exposed surfaces
        // use the same probability as buried blocks, including companion asteroids.
        long h=AsteroidBeltField.hash(field.seed^0x6d94a85cL,x,y,z);
        if(AsteroidBeltField.unit(h)>=scatteredOreChance)return original;
        return scatteredPalette.get((int)(AsteroidBeltField.unit(h+0x52c713L)*scatteredPalette.size()));
    }
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState random,StructureManager structures,ChunkAccess chunk){
        return CompletableFuture.supplyAsync(()->{
            ChunkPos p=chunk.getPos();var bodies=field.intersecting(p.getMinBlockX(),p.getMinBlockZ(),p.getMaxBlockX(),p.getMaxBlockZ());
            LevelChunkSection[] sections=chunk.getSections();for(var section:sections)section.acquire();
            try{
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    BlockState[] column=column(p.getMinBlockX()+x,p.getMinBlockZ()+z,bodies,false);
                    for(int y=0;y<getGenDepth();y++)if(!column[y].isAir())sections[chunk.getSectionIndex(y)].setBlockState(x,y&15,z,column[y],false);
                }
                for(var b:bodies)if(b.type==AsteroidBeltField.Type.GARDEN)for(var v:garden(b).chunks().getOrDefault(AsteroidBeltField.key(p.x,p.z),List.of())){
                    var at=v.pos();var section=sections[chunk.getSectionIndex(at.getY())];
                    if(section.getBlockState(at.getX()&15,at.getY()&15,at.getZ()&15).isAir())section.setBlockState(at.getX()&15,at.getY()&15,at.getZ()&15,v.state(),false);
                }
            }finally{for(var section:sections)section.release();}
            var maps=EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG,Heightmap.Types.OCEAN_FLOOR_WG);maps.addAll(chunk.getStatus().heightmapsAfter());Heightmap.primeHeightmaps(chunk,maps);return chunk;
        },executor);
    }
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor heights,RandomState random){
        var column=column(x,z,field.intersecting(x,z,x,z),false);
        for(int y=Math.min(319,heights.getMaxBuildHeight()-1);y>=Math.max(0,heights.getMinBuildHeight());y--)if(type.isOpaque().test(column[y]))return y+1;
        return heights.getMinBuildHeight();
    }
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor heights,RandomState random){
        var c=column(x,z,field.intersecting(x,z,x,z),true);BlockState[] out=new BlockState[heights.getHeight()];
        for(int i=0;i<out.length;i++){int y=i+heights.getMinBuildHeight();out[i]=y>=0&&y<c.length?c[y]:Blocks.AIR.defaultBlockState();}
        return new NoiseColumn(heights.getMinBuildHeight(),out);
    }
    @Override public void addDebugScreenInfo(List<String> text,RandomState random,BlockPos pos){
        var b=field.major(Math.floorDiv(pos.getX(),AsteroidBeltField.CELL),Math.floorDiv(pos.getZ(),AsteroidBeltField.CELL));text.add("Asteroid belt: "+b.type+" center="+(int)b.x+","+(int)b.y+","+(int)b.z);
    }
}
