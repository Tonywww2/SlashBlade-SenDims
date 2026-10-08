package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import java.util.List;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class WorldTreeSeaChunkGenerator extends ChunkGenerator {
    public static final Codec<WorldTreeSeaChunkGenerator> CODEC=RecordCodecBuilder.create(i->i.group(
        WorldTreeSeaBiomeSource.CODEC.fieldOf("biome_source").forGetter(g->g.source),
        BlockState.CODEC.fieldOf("wood").forGetter(g->g.wood),
        BlockState.CODEC.fieldOf("leaves").forGetter(g->g.leaves),
        BlockState.CODEC.listOf().fieldOf("soils").forGetter(g->g.soils),
        BlockState.CODEC.fieldOf("crystal").forGetter(g->g.crystal),
        BlockState.CODEC.optionalFieldOf("filled_log").forGetter(g->g.filledLog)
    ).apply(i,WorldTreeSeaChunkGenerator::new));
    private final WorldTreeSeaBiomeSource source;
    private final BlockState wood,leaves,crystal;
    private final List<BlockState> soils;
    private final Optional<BlockState> filledLog;
    private final WorldTreeEcology ecology;
    private static final Direction[] FACES={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    public WorldTreeSeaChunkGenerator(WorldTreeSeaBiomeSource source,BlockState wood,BlockState leaves,List<BlockState> soils,BlockState crystal,Optional<BlockState> filledLog){
        super(source);this.source=source;this.wood=wood;this.leaves=leaves;this.soils=List.copyOf(soils);this.crystal=crystal;
        this.filledLog=filledLog;
        this.ecology=new WorldTreeEcology(source.field,wood,leaves,crystal);
        if(soils.isEmpty())throw new IllegalArgumentException("World-tree soils must not be empty");
    }
    public WorldTreeField field(){return source.field;}
    WorldTreeEcology ecology(){return ecology;}
    @Override public void applyBiomeDecoration(WorldGenLevel level,ChunkAccess chunk,StructureManager structures){
        super.applyBiomeDecoration(level,chunk,structures);
        ecology.decorate(level,chunk,structures);
    }
    @Override protected Codec<? extends ChunkGenerator> codec(){return CODEC;}
    @Override public int getGenDepth(){return WorldTreeField.HEIGHT;}
    @Override public int getMinY(){return WorldTreeField.MIN_Y;}
    @Override public int getSeaLevel(){return field().seaLevel;}
    @Override public void applyCarvers(WorldGenRegion region,long seed,RandomState state,BiomeManager manager,StructureManager structures,ChunkAccess chunk,GenerationStep.Carving step){}
    private BlockState state(WorldTreeField.Column c,int y){
        return switch(c.kind(y)) {
            case AIR->Blocks.AIR.defaultBlockState();
            case WATER->Blocks.WATER.defaultBlockState();
            case BEDROCK->Blocks.BEDROCK.defaultBlockState();
            case STONE->Blocks.DEEPSLATE.defaultBlockState();
            case DIRT->Blocks.DIRT.defaultBlockState();
            case SOIL->soils.get(field().materialAt(c.x,c.z,soils.size()));
            case MUD->Blocks.MUD.defaultBlockState();
            case GRAVEL->((field().pointSeed(c.x/5,y/3,c.z/5)&7)==0?Blocks.CLAY:Blocks.GRAVEL).defaultBlockState();
            case WOOD->trunkWood(c,y);
            case LEAVES->leaves;
            case MANGROVE_LOG->Blocks.MANGROVE_LOG.defaultBlockState();
            case MANGROVE_ROOTS->Blocks.MANGROVE_ROOTS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,y<getSeaLevel());
            case MANGROVE_LEAVES->Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT,true).setValue(BlockStateProperties.DISTANCE,7);
            case MANGROVE_PROPAGULE->Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(BlockStateProperties.HANGING,true).setValue(BlockStateProperties.AGE_4,4);
        };
    }
    private BlockState trunkWood(WorldTreeField.Column c,int y){
        if(filledLog.isEmpty())return wood;
        int face=c.filledTrunkFace(y);if(face<0)return wood;
        BlockState s=filledLog.get();
        return s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)?s.setValue(BlockStateProperties.HORIZONTAL_FACING,FACES[face]):s;
    }
    @Override public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor,Blender blender,RandomState randomState,StructureManager structures,ChunkAccess chunk){
        // Each generation task owns its chunk. All geometry is immutable and clipped to these 16x16 columns.
        return CompletableFuture.supplyAsync(()->{
            ChunkPos pos=chunk.getPos();
            List<WorldTreeField.Tree> candidates=field().nearby(pos.getMiddleBlockX(),pos.getMiddleBlockZ());
            // Like vanilla noise generation, own the palette locks and write sections directly.
            // ProtoChunk.setBlockState otherwise scans/updates heightmaps for every block.
            LevelChunkSection[] sections=chunk.getSections();
            for(LevelChunkSection section:sections)section.acquire();
            try{
                for(int z=0;z<16;z++)for(int x=0;x<16;x++){
                    WorldTreeField.Column c=field().cachedColumn(pos.getMinBlockX()+x,pos.getMinBlockZ()+z,candidates);
                    for(int y=getMinY();y<getMinY()+getGenDepth();y++){
                        BlockState s=state(c,y);
                        if(!s.isAir())sections[chunk.getSectionIndex(y)].setBlockState(x,y&15,z,s,false);
                    }
                }
            }finally{for(LevelChunkSection section:sections)section.release();}
            EnumSet<Heightmap.Types> maps=EnumSet.of(Heightmap.Types.WORLD_SURFACE_WG,Heightmap.Types.OCEAN_FLOOR_WG);
            maps.addAll(chunk.getStatus().heightmapsAfter());
            Heightmap.primeHeightmaps(chunk,maps);
            return chunk;
        },executor);
    }
    @Override public void buildSurface(WorldGenRegion region,StructureManager structures,RandomState randomState,ChunkAccess chunk){
        ChunkPos cp=chunk.getPos();BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
        List<WorldTreeField.Tree> nearby=field().nearby(cp.getMiddleBlockX(),cp.getMiddleBlockZ());
        for(int z=0;z<16;z++)for(int x=0;x<16;x++){
            int wx=cp.getMinBlockX()+x,wz=cp.getMinBlockZ()+z;
            WorldTreeField.Column c=field().cachedColumn(wx,wz,nearby);
            long h=field().pointSeed(wx,0,wz)&Long.MAX_VALUE;
            int y=c.floor+1;
            p.set(wx,y,wz);
            double patch=field().vegetationAt(wx,wz);
            if(c.zone==WorldTreeField.Zone.ROOTS){
                if(!chunk.getBlockState(p).isAir()||!chunk.getBlockState(p.below()).is(net.minecraft.tags.BlockTags.DIRT)||h%100>=25+50*patch)continue;
                BlockState plant=(h%17<6?Blocks.FERN:Blocks.GRASS).defaultBlockState();
                if(h%37==0)plant=(h%3==0?Blocks.BLUE_ORCHID:h%3==1?Blocks.LILY_OF_THE_VALLEY:Blocks.AZURE_BLUET).defaultBlockState();
                boolean tall=h%7==0&&patch>.4&&chunk.getBlockState(p.above()).isAir();
                if(tall)plant=(h%3==0?Blocks.LARGE_FERN:Blocks.TALL_GRASS).defaultBlockState();
                if(plant.canSurvive(region,p)){
                    chunk.setBlockState(p,plant,false);
                    if(tall)chunk.setBlockState(p.above(),plant.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER),false);
                }
                continue;
            }
            if(c.kind(y)!=WorldTreeField.Kind.WATER || !chunk.getBlockState(p).is(Blocks.WATER))continue;
            if(c.zone==WorldTreeField.Zone.DEPTHS){
                if(field().mineralPatchAt(wx,wz) && h%23==0)chunk.setBlockState(p,crystal,false);
                else if(h%100<3+12*patch){
                    int height=Math.min(getSeaLevel()-1-y,7+(int)((h>>>8)%22));
                    for(int dy=0;dy<height && y+dy<getSeaLevel()-1;dy++){
                        p.set(wx,y+dy,wz);
                        if(!chunk.getBlockState(p).is(Blocks.WATER))break;
                        // Keep a real kelp head even where a world-tree root interrupts the stalk.
                        boolean end=dy==height-1||!chunk.getBlockState(p.above()).is(Blocks.WATER);
                        chunk.setBlockState(p,(end?Blocks.KELP:Blocks.KELP_PLANT).defaultBlockState(),false);
                        if(end)break;
                    }
                }else if(h%100<22+28*patch)placeSeagrass(chunk,p,h%4==0);
            }else{
                if(h%100<40+35*patch)placeSeagrass(chunk,p,h%3==0);
                if((h>>>12)%100<4+13*patch){
                    p.set(wx,getSeaLevel(),wz);
                    if(chunk.getBlockState(p).isAir()&&chunk.getBlockState(p.below()).is(Blocks.WATER))chunk.setBlockState(p,Blocks.LILY_PAD.defaultBlockState(),false);
                }
            }
        }
    }
    private void placeSeagrass(ChunkAccess chunk,BlockPos.MutableBlockPos p,boolean tall){
        if(tall&&p.getY()+1<getSeaLevel()&&chunk.getBlockState(p.above()).is(Blocks.WATER)){
            chunk.setBlockState(p,Blocks.TALL_SEAGRASS.defaultBlockState(),false);
            chunk.setBlockState(p.above(),Blocks.TALL_SEAGRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER),false);
        }else chunk.setBlockState(p,Blocks.SEAGRASS.defaultBlockState(),false);
    }
    @Override public void spawnOriginalMobs(WorldGenRegion region){
        ChunkPos cp=region.getCenter();
        Holder<Biome> biome=region.getBiome(new BlockPos(cp.getMiddleBlockX(),getSeaLevel(),cp.getMiddleBlockZ()));
        NaturalSpawner.spawnMobsForChunkGeneration(region,biome,cp,RandomSource.create(region.getSeed()^cp.toLong()));
    }
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor access,RandomState randomState){
        WorldTreeField.Column c=field().column(x,z);
        for(int y=Math.min(WorldTreeField.MAX_Y,access.getMaxBuildHeight()-1);y>=Math.max(getMinY(),access.getMinBuildHeight());y--)
            if(type.isOpaque().test(state(c,y)))return y+1;
        return access.getMinBuildHeight();
    }
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor access,RandomState randomState){
        BlockState[] states=new BlockState[access.getHeight()];WorldTreeField.Column c=field().column(x,z);
        for(int i=0;i<states.length;i++)states[i]=state(c,access.getMinBuildHeight()+i);
        return new NoiseColumn(access.getMinBuildHeight(),states);
    }
    @Override public void addDebugScreenInfo(List<String> text,RandomState state,BlockPos pos){
        WorldTreeField.Column c=field().column(pos.getX(),pos.getZ());
        text.add("World tree sea: "+c.zone+" floor="+c.floor+" sea="+getSeaLevel());
    }
    /** Templates are anchored to water/soil rather than WORLD_SURFACE, which legitimately sees the crown. */
    public boolean acceptsWaterStructure(BoundingBox box,boolean city){
        if(box.minY()<getMinY()+1 || box.maxY()>WorldTreeField.MAX_Y)return false;
        int step=city?12:8;
        for(int z=box.minZ();z<=box.maxZ();z+=step)for(int x=box.minX();x<=box.maxX();x+=step){
            WorldTreeField.Column c=field().column(x,z);
            if(city ? c.floor>getSeaLevel()-88 : c.floor>getSeaLevel()-7)return false;
            for(int y=Math.max(c.floor+1,box.minY());y<=Math.min(box.maxY(),getSeaLevel()+70);y+=4)
                if(c.woodAt(y))return false;
        }
        return true;
    }
    public static BlockPos findArrival(ServerLevel level,int x,int z){
        if(!(level.getChunkSource().getGenerator() instanceof WorldTreeSeaChunkGenerator generator))return new BlockPos(x,140,z);
        WorldTreeField f=generator.field();BlockPos best=null;double bestDistance=Double.POSITIVE_INFINITY;
        for(WorldTreeField.Tree t:f.nearby(x,z))for(int i=0;i<24;i++){
            double a=i*Math.PI/12, radius=t.rootIsland*.68;
            int px=(int)Math.round(t.x+Math.cos(a)*radius),pz=(int)Math.round(t.z+Math.sin(a)*radius);
            WorldTreeField.Column c=f.column(px,pz);int py=c.floor+1;
            boolean clear=true;
            for(int dz=-2;dz<=2&&clear;dz++)for(int dx=-2;dx<=2&&clear;dx++){
                WorldTreeField.Column edge=f.column(px+dx,pz+dz);
                if(Math.abs(edge.floor-c.floor)>2||edge.floor<f.seaLevel)clear=false;
                for(int dy=0;dy<8&&clear;dy++)if(edge.kind(py+dy)!=WorldTreeField.Kind.AIR)clear=false;
            }
            double distance=(px-(double)x)*(px-(double)x)+(pz-(double)z)*(pz-(double)z);
            if(clear){
                BoundingBox landing=new BoundingBox(px-2,py,pz-2,px+2,py+7,pz+2);
                for(var plan:generator.ecology.forChunk(new ChunkPos(px>>4,pz>>4)))if(plan.bounds().intersects(landing)){clear=false;break;}
            }
            if(clear&&distance<bestDistance){bestDistance=distance;best=new BlockPos(px,py,pz);}
        }
        if(best==null)throw new IllegalStateException("No safe world-tree arrival within neighboring islands");
        return best;
    }
}
