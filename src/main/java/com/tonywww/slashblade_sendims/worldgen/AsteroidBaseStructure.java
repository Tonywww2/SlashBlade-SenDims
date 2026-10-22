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

/** Jigsaw bases excavated into an asteroid, with a sealed access gallery to the surface. */
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
            if(anchor==null)return Optional.empty();
            int upper=0,lower=0;boolean valid=true;
            for(int i=0;i<pieces.size();i++){
                var p=pieces.get(i);var b=p.getBoundingBox();
                if(i>0){if(b.minY()>pieces.get(0).getBoundingBox().minY()+4)upper++;else lower++;}
            }
            if(upper<1||lower<1)continue;
            // Test the original asteroid, before excavation. Do not inflate or flatten it.
            var core=pieces.get(0);Direction front=core.getRotation().rotate(Direction.NORTH);
            BlockPos placed=anchor;boolean fitted=false;
            for(double offset:new double[]{.22,0,.38}){
                for(int dy:new int[]{0,-7,7}){
                    var site=new BlockPos((int)Math.round(body.x+front.getStepX()*body.rx*offset),(int)Math.round(body.y)-8+dy,(int)Math.round(body.z+front.getStepZ()*body.rz*offset));
                    var shift=site.subtract(placed);for(var p:pieces)p.move(shift.getX(),shift.getY(),shift.getZ());placed=site;
                    if(!buriedSite(body,pieces)||!awayFromGardens(g,pieces))continue;
                    if(!core.accessGallery(g,body))continue;
                    fitted=true;break;
                }
                if(fitted)break;
            }
            if(!fitted)continue;
            for(var p:pieces){
                p.extendBounds();var b=p.getBoundingBox();
                if(b.minY()<8||b.maxY()>310||(b.minX()>>4)<cp.x-8||(b.maxX()>>4)>cp.x+8||(b.minZ()>>4)<cp.z-8||(b.maxZ()>>4)>cp.z+8){valid=false;break;}
            }
            if(!valid)continue;
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
            BlockPos site=placed;
            return Optional.of(new GenerationStub(site,b->pieces.forEach(b::addPiece)));
        }
        return Optional.empty();
    }
    private static boolean buriedSite(AsteroidBeltField.Body body,List<Piece> pieces){
        int total=0,solid=0;
        for(int i=0;i<pieces.size();i++){
            var p=pieces.get(i);int count=0,inside=0;
            for(int z=i==0?4:2;z<=(i==0?20:16);z+=3)for(int x=i==0?4:2;x<=(i==0?20:14);x+=3)
                for(int y=3;y<=(i==0?16:8);y+=3){
                    var at=p.local(x,y,z);count++;
                    if(body.sample(at.getX(),at.getY(),at.getZ())!=AsteroidBeltField.Material.AIR)inside++;
                }
            if(inside<count*(i==0?.88:.40))return false;
            total+=count;solid+=inside;
        }
        return solid>=total*.80;
    }
    private static boolean awayFromGardens(AsteroidBeltChunkGenerator g,List<Piece> pieces){
        for(var p:pieces){var b=p.templateBox();for(var body:g.field().intersecting(b.minX(),b.minZ(),b.maxX(),b.maxZ()))
            if(body.type==AsteroidBeltField.Type.GARDEN&&body.maxY()>=b.minY()&&body.minY()<=b.maxY())return false;}
        return true;
    }
    private static StructurePlaceSettings settings(Rotation rotation){return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true).setKeepLiquids(false).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);}
    public static final class Piece extends TemplateStructurePiece {
        private final Set<BlockPos> open=new HashSet<>();
        private final Map<BlockPos,Integer> piles=new HashMap<>();
        private int galleryLength;
        public Piece(StructureTemplateManager manager,ResourceLocation id,BlockPos at,Rotation rotation){
            super(SaturnRingWorldGenRegistry.ASTEROID_BASE_PIECE.get(),0,manager,id,id.toString(),settings(rotation),at);
        }
        public Piece(StructurePieceSerializationContext c,CompoundTag tag){
            super(SaturnRingWorldGenRegistry.ASTEROID_BASE_PIECE.get(),tag,c.structureTemplateManager(),id->settings(Rotation.valueOf(tag.getString("Rot"))));
            for(long p:tag.getLongArray("Open"))open.add(BlockPos.of(p));
            for(Tag raw:tag.getList("Piles",10)){var t=(CompoundTag)raw;piles.put(BlockPos.of(t.getLong("Pos")),t.getInt("Y"));}
            galleryLength=tag.getInt("GalleryLength");extendBounds();
        }
        public List<StructureTemplate.StructureBlockInfo> ports(){return template.filterBlocks(templatePosition,placeSettings.copy().setBoundingBox(null),Blocks.JIGSAW);}
        public List<StructureTemplate.StructureBlockInfo> markers(){return template.filterBlocks(templatePosition,placeSettings.copy().setBoundingBox(null),Blocks.STRUCTURE_BLOCK);}
        public Set<BlockPos> openPorts(){return Set.copyOf(open);}
        public String templateId(){return templateName;}
        public int galleryLength(){return galleryLength;}
        public BlockPos local(int x,int y,int z){return StructureTemplate.transform(new BlockPos(x,y,z),Mirror.NONE,getRotation(),BlockPos.ZERO).offset(templatePosition);}
        private BoundingBox templateBox(){return template.getBoundingBox(placeSettings,templatePosition);}
        private boolean accessGallery(AsteroidBeltChunkGenerator g,AsteroidBeltField.Body host){
            galleryLength=0;var a=local(1,1,0);var end=local(9,15,-112);
            var bodies=g.field().intersecting(Math.min(a.getX(),end.getX()),Math.min(a.getZ(),end.getZ()),Math.max(a.getX(),end.getX()),Math.max(a.getZ(),end.getZ()));
            int lastHost=0;boolean[] occupied=new boolean[113],garden=new boolean[113];
            // A full cross-section scan avoids a false exit inside a hollow or a companion.
            for(int d=1;d<=112;d++)for(int x=1;x<=9;x++)for(int y=1;y<=12;y++){
                var at=local(x,y,-d);
                for(var b:bodies){
                    if(at.getY()<b.minY()||at.getY()>b.maxY())continue;
                    if(b.type==AsteroidBeltField.Type.GARDEN&&b.inGarden(at.getX(),at.getY(),at.getZ()))garden[d]=true;
                    if(b.sample(at.getX(),at.getY(),at.getZ())!=AsteroidBeltField.Material.AIR){
                        if(b.type==AsteroidBeltField.Type.GARDEN)garden[d]=true;
                        if(b.id==host.id&&b.x==host.x&&b.z==host.z)lastHost=d;
                        occupied[d]=true;
                    }
                }
            }
            // Stop at the first usable outside gap after the host's last surface.
            // A distant companion must not pull the gallery across empty space.
            for(int length=Math.max(8,lastHost+4);length<=96;length++){
                boolean clear=true;for(int d=length-3;d<=length+4;d++)if(occupied[d]){clear=false;break;}
                if(!clear)continue;
                for(int d=1;d<=length+3;d++)if(garden[d])return false;
                galleryLength=length;return true;
            }
            return false;
        }
        private void supports(AsteroidBeltChunkGenerator g){
            for(var m:markers())if(m.nbt()!=null&&Set.of("foot","access").contains(m.nbt().getString("metadata"))){
                var p=m.pos();var col=g.column(p.getX(),p.getZ(),g.field().intersecting(p.getX(),p.getZ(),p.getX(),p.getZ()),false);
                for(int y=p.getY()-1;y>=Math.max(4,p.getY()-72);y--)if(!col[y].isAir()&&col[y].getFluidState().isEmpty()){piles.put(p,y);break;}
            }
        }
        private void extendBounds(){
            var b=template.getBoundingBox(placeSettings,templatePosition);int min=b.minY();for(int y:piles.values())min=Math.min(min,y);
            boundingBox=new BoundingBox(b.minX(),min,b.minZ(),b.maxX(),b.maxY(),b.maxZ());
            if(galleryLength>0)for(int x:new int[]{1,9})for(int y:new int[]{1,12})for(int z:new int[]{0,-galleryLength-3})boundingBox.encapsulate(local(x,y,z));
        }
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){
            super.addAdditionalSaveData(c,tag);tag.putString("Rot",getRotation().name());tag.putLongArray("Open",open.stream().mapToLong(BlockPos::asLong).sorted().toArray());
            ListTag list=new ListTag();piles.forEach((p,y)->{CompoundTag t=new CompoundTag();t.putLong("Pos",p.asLong());t.putInt("Y",y);list.add(t);});tag.put("Piles",list);
            tag.putInt("GalleryLength",galleryLength);
        }
        @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            super.postProcess(level,manager,generator,random,clip,chunk,pivot);
            if(galleryLength>0)placeGallery(level,clip);
            for(var port:ports())if(!open.contains(port.pos())){
                Direction side=JigsawBlock.getFrontFacing(port.state()).getClockWise();
                for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<3;dy++){
                    var p=port.pos().relative(side,dx).above(dy);if(clip.isInside(p))level.setBlock(p,dy==1?Blocks.TINTED_GLASS.defaultBlockState():plate(),2);
                }
            }
            extendBounds();
        }
        private static BlockState plate(){return BuiltInRegistries.BLOCK.get(new ResourceLocation("ad_astra:steel_plating")).defaultBlockState();}
        private static BlockState block(String name){return BuiltInRegistries.BLOCK.get(new ResourceLocation(name)).defaultBlockState();}
        private void put(WorldGenLevel level,BoundingBox clip,int x,int y,int z,BlockState state){var at=local(x,y,z);if(clip.isInside(at))level.setBlock(at,state,2);}
        private void placeGallery(WorldGenLevel level,BoundingBox clip){
            boolean ruined=templateName.contains("_ruined");
            for(int d=0;d<=galleryLength;d++)for(int x=3;x<=7;x++)for(int y=1;y<=7;y++){
                boolean shell=x==3||x==7||y<=2||y>=6;
                BlockState state=shell?plate():Blocks.AIR.defaultBlockState();
                if(shell&&d%6==0)state=block("quark:iron_plate");
                if(y==5&&x==5&&d%8==3)state=Blocks.SEA_LANTERN.defaultBlockState();
                if(d>=galleryLength-5&&(x==3||x==7)&&y==4)state=Blocks.TINTED_GLASS.defaultBlockState();
                // Rust and loose plates stay outside the three-wide walking lane.
                if(ruined&&y==2&&(x==3||x==7)&&d%5<2)state=block("quark:rusty_iron_plate");
                put(level,clip,x,y,-d,state);
            }
            // Compact exposed arrival ledge; rails and recessed lights frame the tunnel mouth.
            for(int d=galleryLength-2;d<=galleryLength+3;d++)for(int x=1;x<=9;x++){
                put(level,clip,x,2,-d,plate());
                if(x==1||x==9){put(level,clip,x,3,-d,plate());put(level,clip,x,4,-d,Blocks.IRON_BARS.defaultBlockState());}
            }
            for(int x:new int[]{2,8}){for(int y=3;y<=8;y++)put(level,clip,x,y,-galleryLength,plate());put(level,clip,x,8,-galleryLength,Blocks.SEA_LANTERN.defaultBlockState());}
            for(int x=2;x<=8;x++)put(level,clip,x,7,-galleryLength,plate());
            if(!ruined||templateName.endsWith("core_ruined1")){
                put(level,clip,5,8,-galleryLength+1,block("ad_astra:solar_panel").rotate(getRotation()));
            }
            for(int y=8;y<=(ruined?9:11);y++)put(level,clip,7,y,-galleryLength+3,plate());
            put(level,clip,7,ruined?10:12,-galleryLength+3,Blocks.SEA_LANTERN.defaultBlockState());
        }
        @Override protected void handleDataMarker(String label,BlockPos at,ServerLevelAccessor level,RandomSource random,BoundingBox clip){
            if(!clip.isInside(at))return;
            if(label.equals("breach")){level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return;}
            if(label.equals("anchor")){level.setBlock(at,Blocks.AIR.defaultBlockState(),2);return;}
            if(!label.equals("foot")&&!label.equals("access"))return;
            if(label.equals("access")){
                if(galleryLength>0){level.setBlock(at,plate(),2);return;}
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
