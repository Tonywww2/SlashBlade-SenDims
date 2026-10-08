package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import java.util.*;

/** Sparse template starts anchored to the actual tree/shore field, never the canopy heightmap. */
public final class WorldTreeSmallStructure extends Structure {
    public static final Codec<WorldTreeSmallStructure> CODEC=RecordCodecBuilder.create(i->i.group(
        settingsCodec(i),Codec.STRING.fieldOf("kind").forGetter(s->s.kind),
        ResourceLocation.CODEC.listOf().fieldOf("templates").forGetter(s->s.templates),
        BlockPos.CODEC.fieldOf("anchor").forGetter(s->s.anchor)
    ).apply(i,WorldTreeSmallStructure::new));
    private final String kind;
    private final List<ResourceLocation> templates;
    private final BlockPos anchor;
    public WorldTreeSmallStructure(StructureSettings settings,String kind,List<ResourceLocation> templates,BlockPos anchor){
        super(settings);this.kind=kind;this.templates=List.copyOf(templates);this.anchor=anchor;
        if(templates.isEmpty()||!Set.of("root","shrine","shore","depth","canopy","bridge").contains(kind))
            throw new IllegalArgumentException("Invalid world-tree small structure definition");
    }
    public String kind(){return kind;}
    @Override public StructureType<?> type(){return SaturnRingWorldGenRegistry.WORLD_TREE_SMALL.get();}

    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        if(!(c.chunkGenerator() instanceof WorldTreeSeaChunkGenerator g))return Optional.empty();
        var f=g.field();var random=c.random();
        ResourceLocation id=templates.get(random.nextInt(templates.size()));
        var template=c.structureTemplateManager().getOrCreate(id);
        if(template.getSize().getX()==0)return Optional.empty();
        // Fixed work bound per sparse candidate chunk; no chunk reads, global scans or world writes.
        for(int attempt=0;attempt<16;attempt++){
            int x=c.chunkPos().getMinBlockX()+random.nextInt(16),z=c.chunkPos().getMinBlockZ()+random.nextInt(16);
            Rotation rotation=Rotation.values()[random.nextInt(4)];
            BlockPos site=site(f,x,z,rotation);
            if(site==null)continue;
            BlockPos transformed=StructureTemplate.transform(anchor,Mirror.NONE,rotation,BlockPos.ZERO);
            BlockPos origin=site.subtract(transformed);
            var box=template.getBoundingBox(settings(rotation),origin);
            if(box.minY()<WorldTreeField.MIN_Y+1||box.maxY()>WorldTreeField.MAX_Y)continue;
            if(!clearCore(f,site,rotation))continue;
            return Optional.of(new GenerationStub(site,b->b.addPiece(new Piece(c.structureTemplateManager(),id,origin,rotation))));
        }
        return Optional.empty();
    }
    private BlockPos site(WorldTreeField f,int x,int z,Rotation r){
        var col=f.column(x,z);int floor=col.floor;
        if(kind.equals("canopy")){
            int y=branchTop(col);if(y<170||y>280)return null;
            // Four corner piles reach real wood within 12 blocks, not leaves.
            for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3}){
                var p=offset(x,0,z,dx,dz,r);int h=branchTop(f.column(p.getX(),p.getZ()));
                if(h<y-11||h>y+3)return null;
            }
            return new BlockPos(x,y+4,z);
        }
        if(kind.equals("bridge")){
            // Try lower coherent branches as well as the highest one. Dense canopy sites are rejected.
            List<WorldTreeField.Column> feet=new ArrayList<>();
            for(int dx:new int[]{-16,-11,11,16})for(int dz:new int[]{-3,3}){
                var p=offset(x,0,z,dx,dz,r);feet.add(f.column(p.getX(),p.getZ()));
            }
            int previous=-1;
            for(int ceiling=250;ceiling>=180;ceiling-=4){
                int highest=-1,lowest=320;
                for(var foot:feet){int h=branchBelow(foot,ceiling);highest=Math.max(highest,h);lowest=Math.min(lowest,h);}
                if(lowest<170||highest-lowest>10||highest==previous)continue;
                previous=highest;var at=new BlockPos(x,highest+4,z);
                if(bridgeClearance(f,at,r))return at;
            }
            return null;
        }
        if(kind.equals("depth")){
            if(col.zone!=WorldTreeField.Zone.DEPTHS||floor>f.seaLevel-26)return null;
            int high=floor,low=floor;
            for(int dx:new int[]{-5,0,5})for(int dz:new int[]{-6,0,6}){
                var p=offset(x,0,z,dx,dz,r);var v=f.column(p.getX(),p.getZ());
                if(v.zone!=WorldTreeField.Zone.DEPTHS)return null;
                high=Math.max(high,v.floor);low=Math.min(low,v.floor);
            }
            if(high-low>6)return null;
            return new BlockPos(x,high+2,z);
        }
        if(kind.equals("shore")){
            // Template +Z points into the sea, -Z into the dry reed shore.
            var back=offset(x,0,z,0,-5,r);var front=offset(x,0,z,0,13,r);
            int land=f.floorAt(back.getX(),back.getZ()),water=f.floorAt(front.getX(),front.getZ());
            if(land<f.seaLevel-1||land>f.seaLevel+3||water>f.seaLevel-2||water<f.seaLevel-10)return null;
            return new BlockPos(x,Math.max(f.seaLevel+2,land+1),z);
        }
        if(col.zone!=WorldTreeField.Zone.ROOTS)return null;
        int high=floor,low=floor;
        for(int dx:new int[]{-5,0,5})for(int dz:new int[]{-6,0,6}){
            var p=offset(x,0,z,dx,dz,r);int h=f.floorAt(p.getX(),p.getZ());
            high=Math.max(high,h);low=Math.min(low,h);
        }
        if(low<f.seaLevel||high-low>3)return null;
        boolean root=false;
        for(int dx:new int[]{-18,0,18})for(int dz:new int[]{-18,0,18}){
            var v=f.column(x+dx,z+dz);
            for(int y=high+2;y<high+17;y+=4)if(v.woodAt(y))root=true;
        }
        return root?new BlockPos(x,high+1,z):null;
    }
    private boolean clearCore(WorldTreeField f,BlockPos site,Rotation r){
        int range=kind.equals("bridge")?14:4;
        for(int dx=-range;dx<=range;dx+=kind.equals("bridge")?7:4)for(int dz=-4;dz<=4;dz+=4){
            var p=offset(site.getX(),site.getY(),site.getZ(),dx,dz,r);var col=f.column(p.getX(),p.getZ());
            for(int y=site.getY()+1;y<=site.getY()+7;y+=2)if(col.woodAt(y))return false;
        }
        return true;
    }
    private static int branchTop(WorldTreeField.Column col){
        for(int y=284;y>=170;y--)if(col.woodAt(y))return y;
        return -1;
    }
    private static int branchBelow(WorldTreeField.Column col,int ceiling){
        for(int y=ceiling;y>=170;y--)if(col.woodAt(y))return y;
        return -1;
    }
    private static boolean bridgeClearance(WorldTreeField f,BlockPos site,Rotation r){
        int leaves=0,samples=0;
        for(int dx=-16;dx<=16;dx+=2)for(int dz=-3;dz<=3;dz+=3){
            var p=offset(site.getX(),0,site.getZ(),dx,dz,r);var col=f.column(p.getX(),p.getZ());
            for(int y=site.getY()-1;y<=site.getY()+8;y++){
                if(col.woodAt(y))return false;
                if(col.leavesAt(y))leaves++;samples++;
            }
        }
        return leaves*100<=samples*12;
    }
    private static BlockPos offset(int x,int y,int z,int dx,int dz,Rotation r){
        return new BlockPos(x,y,z).offset(StructureTemplate.transform(new BlockPos(dx,0,dz),Mirror.NONE,r,BlockPos.ZERO));
    }
    private static StructurePlaceSettings settings(Rotation r){
        return new StructurePlaceSettings().setRotation(r).setIgnoreEntities(true).setKeepLiquids(false)
            .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }
    public static final class Piece extends TemplateStructurePiece {
        public Piece(StructureTemplateManager manager,ResourceLocation id,BlockPos origin,Rotation r){
            super(SaturnRingWorldGenRegistry.WORLD_TREE_SMALL_PIECE.get(),0,manager,id,id.toString(),settings(r),origin);
            extendFeetBounds();
        }
        public Piece(StructurePieceSerializationContext c,CompoundTag tag){
            super(SaturnRingWorldGenRegistry.WORLD_TREE_SMALL_PIECE.get(),tag,c.structureTemplateManager(),
                id->settings(Rotation.valueOf(tag.getString("Rot"))));extendFeetBounds();
        }
        private void extendFeetBounds(){
            var b=boundingBox;boundingBox=new BoundingBox(b.minX(),Math.max(WorldTreeField.MIN_Y+1,b.minY()-16),b.minZ(),b.maxX(),b.maxY(),b.maxZ());
        }
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){
            super.addAdditionalSaveData(c,tag);tag.putString("Rot",getRotation().name());
        }
        @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            super.postProcess(level,manager,generator,random,clip,chunk,pivot);extendFeetBounds();
            if(templateName.contains("broken_vine_bridge")){
                // Trim only foliage in the walking/head space. Keep tree wood and the broken deck gap.
                for(int x=1;x<=33;x++)for(int z=1;z<=7;z++)for(int y=3;y<=10;y++){
                    var at=templatePosition.offset(StructureTemplate.transform(new BlockPos(x,y,z),Mirror.NONE,getRotation(),BlockPos.ZERO));
                    if(clip.isInside(at)&&level.getBlockState(at).is(BlockTags.LEAVES))level.setBlock(at,Blocks.AIR.defaultBlockState(),2);
                }
            }
        }
        @Override protected void handleDataMarker(String marker,BlockPos p,ServerLevelAccessor level,RandomSource random,BoundingBox clip){
            if(!marker.equals("foot")||!clip.isInside(p))return;
            BlockState timber=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(new ResourceLocation("integrateddynamics:menril_log")).defaultBlockState();
            if(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(p.above()).getBlock()).toString().equals("quark:iron_plate"))
                timber=level.getBlockState(p.above());
            for(int depth=0;depth<=16&&p.getY()-depth>level.getMinBuildHeight();depth++){
                var at=p.below(depth);if(!clip.isInside(at))continue;
                var old=level.getBlockState(at);
                if(depth>0&&!old.isAir()&&!old.canBeReplaced()&&!old.is(BlockTags.LEAVES)&&old.getFluidState().isEmpty())break;
                level.setBlock(at,timber,2);
            }
        }
    }
}
