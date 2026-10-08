package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import java.util.*;

/** Real vanilla jigsaw assembly, with shared three-wide ports and terrain-aware foundation piles. */
public final class AsteroidBaseStructure extends Structure {
    public static final Codec<AsteroidBaseStructure> CODEC=RecordCodecBuilder.create(i->i.group(
        settingsCodec(i),StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s->s.pool),
        StructureTemplatePool.CODEC.optionalFieldOf("intact_start_pool").forGetter(s->s.intactPool),
        Codec.doubleRange(0,1).optionalFieldOf("intact_chance",.02).forGetter(s->s.intactChance)
    ).apply(i,AsteroidBaseStructure::new));
    private final Holder<StructureTemplatePool> pool;
    private final Optional<Holder<StructureTemplatePool>> intactPool;
    private final double intactChance;
    public AsteroidBaseStructure(StructureSettings settings,Holder<StructureTemplatePool> pool,Optional<Holder<StructureTemplatePool>> intactPool,double intactChance){super(settings);this.pool=pool;this.intactPool=intactPool;this.intactChance=intactChance;}
    public boolean intactAt(long seed,ChunkPos pos){return intactPool.isPresent()&&AsteroidBeltField.unit(AsteroidBeltField.hash(seed^0x4e8247L,pos.x,0,pos.z))<intactChance;}
    @Override public StructureType<?> type(){return SaturnRingWorldGenRegistry.ASTEROID_BASE.get();}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        if(!(c.chunkGenerator() instanceof AsteroidBeltChunkGenerator g))return Optional.empty();
        var cp=c.chunkPos();AsteroidBeltField.Body body=g.field().major(Math.floorDiv(cp.getMiddleBlockX(),AsteroidBeltField.CELL),Math.floorDiv(cp.getMiddleBlockZ(),AsteroidBeltField.CELL));
        if(body.type==AsteroidBeltField.Type.GARDEN||Math.min(body.rx,body.rz)<35||Math.abs(body.x-cp.getMiddleBlockX())>80||Math.abs(body.z-cp.getMiddleBlockZ())>80)return Optional.empty();
        int deck=0;
        for(int dz=-12;dz<=12;dz+=6)for(int dx=-12;dx<=12;dx+=6){
            int x=(int)Math.round(body.x)+dx,z=(int)Math.round(body.z)+dz;
            for(int y=body.maxY();y>=body.minY();y--)if(body.sample(x,y,z)!=AsteroidBeltField.Material.AIR){deck=Math.max(deck,y+2);break;}
        }
        BlockPos site=new BlockPos((int)Math.round(body.x),deck+1,(int)Math.round(body.z));
        // Choose once per candidate, before retries, so failed layouts cannot reroll the rare condition.
        var selectedPool=intactAt(c.seed(),cp)?intactPool.orElse(pool):pool;
        for(int attempt=0;attempt<3;attempt++){
            var stub=JigsawPlacement.addPieces(c,selectedPool,Optional.empty(),1,new BlockPos(0,128,0),false,Optional.empty(),90);
            if(stub.isEmpty())continue;
            var originals=stub.get().getPiecesBuilder().build().pieces();if(originals.size()<4)continue;
            List<Piece> pieces=new ArrayList<>();var ops=RegistryOps.create(NbtOps.INSTANCE,c.registryAccess());
            for(var raw:originals){
                if(!(raw instanceof PoolElementStructurePiece p))return Optional.empty();
                var encoded=StructurePoolElement.CODEC.encodeStart(ops,p.getElement()).result();
                if(encoded.isEmpty()||!(encoded.get() instanceof CompoundTag tag)||!tag.contains("location"))return Optional.empty();
                pieces.add(new Piece(c.structureTemplateManager(),new ResourceLocation(tag.getString("location")),p.getPosition(),p.getRotation()));
            }
            BlockPos anchor=null;for(var marker:pieces.get(0).markers())if(marker.nbt()!=null&&marker.nbt().getString("metadata").equals("anchor"))anchor=marker.pos();
            if(anchor==null)return Optional.empty();BlockPos shift=site.subtract(anchor);
            for(var p:pieces)p.move(shift.getX(),shift.getY(),shift.getZ());
            int upper=0,lower=0;boolean valid=true;
            for(int i=0;i<pieces.size();i++){
                var p=pieces.get(i);var b=p.getBoundingBox();
                if(b.minY()<8||b.maxY()>310||(b.minX()>>4)<cp.x-8||(b.maxX()>>4)>cp.x+8||(b.minZ()>>4)<cp.z-8||(b.maxZ()>>4)>cp.z+8){valid=false;break;}
                if(i>0){if(b.minY()>pieces.get(0).getBoundingBox().minY()+4)upper++;else lower++;}
                p.supports(g);
            }
            if(!valid||upper<1||lower<1||!clearSite(g,pieces))continue;
            Map<BlockPos,StructureTemplate.StructureBlockInfo> ports=new HashMap<>();for(var p:pieces)for(var port:p.ports())ports.put(port.pos(),port);
            for(var p:pieces)for(var port:p.ports()){
                Direction d=JigsawBlock.getFrontFacing(port.state());var other=ports.get(port.pos().relative(d));
                if(other!=null&&JigsawBlock.getFrontFacing(other.state())==d.getOpposite())p.open.add(port.pos());
            }
            // Clip upper foundations at an intervening module roof before they cross any room.
            for(var p:pieces)for(var entry:p.piles.entrySet())for(var other:pieces){
                if(other==p)continue;var b=other.templateBox();var at=entry.getKey();
                if(at.getX()>b.minX()&&at.getX()<b.maxX()&&at.getZ()>b.minZ()&&at.getZ()<b.maxZ()&&b.maxY()<at.getY()&&b.maxY()>entry.getValue())entry.setValue(b.maxY());
            }
            for(var p:pieces)p.extendBounds();
            return Optional.of(new GenerationStub(site,b->pieces.forEach(b::addPiece)));
        }
        return Optional.empty();
    }
    private static boolean clearSite(AsteroidBeltChunkGenerator g,List<Piece> pieces){
        // The denser belt has larger companions: reject occupied rooms and roof clearance.
        for(var p:pieces){
            var box=p.templateBox();var bodies=g.field().intersecting(box.minX()-2,box.minZ()-2,box.maxX()+2,box.maxZ()+2);
            for(var body:bodies){
                if(body.maxY()<box.minY()+3||body.minY()>box.maxY()+4)continue;
                int minY=Math.max(box.minY()+3,body.minY()),maxY=Math.min(box.maxY()+4,body.maxY());
                for(int z=box.minZ()-2;z<=box.maxZ()+2;z++)for(int x=box.minX()-2;x<=box.maxX()+2;x++)
                    for(int y=minY;y<=maxY;y++)if(body.sample(x,y,z)!=AsteroidBeltField.Material.AIR)return false;
            }
        }
        return true;
    }
    private static StructurePlaceSettings settings(Rotation rotation){return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true).setKeepLiquids(false).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);}
    public static final class Piece extends TemplateStructurePiece {
        private final Set<BlockPos> open=new HashSet<>();
        private final Map<BlockPos,Integer> piles=new HashMap<>();
        public Piece(StructureTemplateManager manager,ResourceLocation id,BlockPos at,Rotation rotation){
            super(SaturnRingWorldGenRegistry.ASTEROID_BASE_PIECE.get(),0,manager,id,id.toString(),settings(rotation),at);
        }
        public Piece(StructurePieceSerializationContext c,CompoundTag tag){
            super(SaturnRingWorldGenRegistry.ASTEROID_BASE_PIECE.get(),tag,c.structureTemplateManager(),id->settings(Rotation.valueOf(tag.getString("Rot"))));
            for(long p:tag.getLongArray("Open"))open.add(BlockPos.of(p));
            for(Tag raw:tag.getList("Piles",10)){var t=(CompoundTag)raw;piles.put(BlockPos.of(t.getLong("Pos")),t.getInt("Y"));}extendBounds();
        }
        public List<StructureTemplate.StructureBlockInfo> ports(){return template.filterBlocks(templatePosition,placeSettings.copy().setBoundingBox(null),Blocks.JIGSAW);}
        public List<StructureTemplate.StructureBlockInfo> markers(){return template.filterBlocks(templatePosition,placeSettings.copy().setBoundingBox(null),Blocks.STRUCTURE_BLOCK);}
        public Set<BlockPos> openPorts(){return Set.copyOf(open);}
        public String templateId(){return templateName;}
        private BoundingBox templateBox(){return template.getBoundingBox(placeSettings,templatePosition);}
        private void supports(AsteroidBeltChunkGenerator g){
            for(var m:markers())if(m.nbt()!=null&&Set.of("foot","access").contains(m.nbt().getString("metadata"))){
                var p=m.pos();var col=g.column(p.getX(),p.getZ(),g.field().intersecting(p.getX(),p.getZ(),p.getX(),p.getZ()),false);
                for(int y=p.getY()-1;y>=Math.max(4,p.getY()-72);y--)if(!col[y].isAir()&&col[y].getFluidState().isEmpty()){piles.put(p,y);break;}
            }
        }
        private void extendBounds(){var b=template.getBoundingBox(placeSettings,templatePosition);int min=b.minY();for(int y:piles.values())min=Math.min(min,y);boundingBox=new BoundingBox(b.minX(),min,b.minZ(),b.maxX(),b.maxY(),b.maxZ());}
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){
            super.addAdditionalSaveData(c,tag);tag.putString("Rot",getRotation().name());tag.putLongArray("Open",open.stream().mapToLong(BlockPos::asLong).sorted().toArray());
            ListTag list=new ListTag();piles.forEach((p,y)->{CompoundTag t=new CompoundTag();t.putLong("Pos",p.asLong());t.putInt("Y",y);list.add(t);});tag.put("Piles",list);
        }
        @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            super.postProcess(level,manager,generator,random,clip,chunk,pivot);
            for(var port:ports())if(!open.contains(port.pos())){
                Direction side=JigsawBlock.getFrontFacing(port.state()).getClockWise();
                for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<3;dy++){
                    var p=port.pos().relative(side,dx).above(dy);if(clip.isInside(p))level.setBlock(p,dy==1?Blocks.TINTED_GLASS.defaultBlockState():plate(),2);
                }
            }
            extendBounds();
        }
        private static BlockState plate(){return BuiltInRegistries.BLOCK.get(new ResourceLocation("ad_astra:steel_plating")).defaultBlockState();}
        @Override protected void handleDataMarker(String label,BlockPos at,ServerLevelAccessor level,RandomSource random,BoundingBox clip){
            if(!clip.isInside(at))return;
            if(label.equals("breach")){level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return;}
            if(label.equals("anchor")){level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return;}
            if(!label.equals("foot")&&!label.equals("access"))return;
            if(label.equals("access")){
                Integer floor=piles.get(at);if(floor==null){level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return;}
                Direction facing=getRotation().rotate(Direction.NORTH);
                var ladder=BuiltInRegistries.BLOCK.get(new ResourceLocation("quark:iron_ladder")).defaultBlockState().setValue(LadderBlock.FACING,facing);
                for(int y=at.getY();y>floor;y--){var p=new BlockPos(at.getX(),y,at.getZ());if(clip.isInside(p)){level.setBlock(p.relative(facing.getOpposite()),plate(),2);level.setBlock(p,ladder,2);}}
                return;
            }
            level.setBlock(at,plate(),2);
            Integer floor=piles.get(at);if(floor==null)return;
            for(int y=at.getY()-1;y>floor;y--){BlockPos p=new BlockPos(at.getX(),y,at.getZ());if(clip.isInside(p))level.setBlock(p,plate(),2);}
        }
    }
}
