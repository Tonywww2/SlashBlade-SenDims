package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
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

/** Native weighted jigsaw assembly, followed by tree/sea anchoring, route validation and port closure. */
public final class WorldTreeDungeonStructure extends Structure {
    public static final Codec<WorldTreeDungeonStructure> CODEC=RecordCodecBuilder.create(i->i.group(
        settingsCodec(i),Codec.STRING.fieldOf("kind").forGetter(s->s.kind),
        StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s->s.startPool),
        Codec.intRange(1,64).fieldOf("min_pieces").forGetter(s->s.minPieces),
        Codec.intRange(1,64).fieldOf("max_pieces").forGetter(s->s.maxPieces),
        Codec.intRange(1,8).fieldOf("chest_budget").forGetter(s->s.chestBudget)
    ).apply(i,WorldTreeDungeonStructure::new));
    private final String kind;
    private final Holder<StructureTemplatePool> startPool;
    private final int minPieces,maxPieces,chestBudget;
    public WorldTreeDungeonStructure(StructureSettings settings,String kind,Holder<StructureTemplatePool> pool,int min,int max,int chests){
        super(settings);this.kind=kind;startPool=pool;minPieces=min;maxPieces=max;chestBudget=chests;
        if(!Set.of("canopy","hollow","abyss").contains(kind)||max<min)throw new IllegalArgumentException("Invalid world tree dungeon settings");
    }
    public String kind(){return kind;}
    @Override public StructureType<?> type(){return SaturnRingWorldGenRegistry.WORLD_TREE_DUNGEON.get();}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        if(!(c.chunkGenerator() instanceof WorldTreeSeaChunkGenerator g))return Optional.empty();
        WorldTreeField f=g.field();
        // All expensive work is gated by a sparse structure-set candidate and a cheap site check.
        for(int location=0;location<3;location++){
            int x=c.chunkPos().getMinBlockX()+c.random().nextInt(16),z=c.chunkPos().getMinBlockZ()+c.random().nextInt(16);
            BlockPos site=site(f,x,z);if(site==null)continue;
            for(int variant=0;variant<4;variant++){
                var layout=assemble(c,site,f);
                if(layout.isPresent())return layout;
            }
        }
        return Optional.empty();
    }
    private BlockPos site(WorldTreeField f,int x,int z){
        var col=f.column(x,z);
        if(kind.equals("canopy")){
            for(int y=238;y>=175;y--)if(col.woodAt(y)){
                for(int above=y+1;above<=Math.min(310,y+20);above++)if(col.woodAt(above))return null;
                return new BlockPos(x,y+4,z);
            }
            return null;
        }
        if(kind.equals("abyss"))return col.zone==WorldTreeField.Zone.DEPTHS&&col.floor<f.seaLevel-24?new BlockPos(x,col.floor+2,z):null;
        if(col.zone!=WorldTreeField.Zone.ROOTS)return null;
        for(int dx:new int[]{-2,0,2})for(int dz:new int[]{-2,0,2}){
            var edge=f.column(x+dx,z+dz);if(Math.abs(edge.floor-col.floor)>2)return null;
            for(int y=col.floor+1;y<=col.floor+5;y+=2)if(edge.woodAt(y))return null;
        }
        boolean root=false;
        for(int dx:new int[]{-24,0,24})for(int dz:new int[]{-24,0,24}){
            var edge=f.column(x+dx,z+dz);if(edge.woodAt(col.floor+12))root=true;
        }
        return root?new BlockPos(x,col.floor+1,z):null;
    }
    private Optional<GenerationStub> assemble(GenerationContext c,BlockPos site,WorldTreeField f){
        // The vanilla assembler controls weighted choices, rotations, collision and recursive expansion.
        var nativeStub=JigsawPlacement.addPieces(c,startPool,Optional.empty(),14,new BlockPos(0,128,0),false,Optional.empty(),128);
        if(nativeStub.isEmpty())return Optional.empty();
        List<StructurePiece> nativePieces=nativeStub.get().getPiecesBuilder().build().pieces();
        if(nativePieces.size()<minPieces||nativePieces.size()>maxPieces)return Optional.empty();
        List<Piece> pieces=new ArrayList<>();
        var ops=RegistryOps.create(NbtOps.INSTANCE,c.registryAccess());
        int chests=0,cores=0,hubs=0,mains=0;
        for(var raw:nativePieces){
            if(!(raw instanceof PoolElementStructurePiece p))return Optional.empty();
            var encoded=StructurePoolElement.CODEC.encodeStart(ops,p.getElement()).result();
            if(encoded.isEmpty()||!(encoded.get() instanceof CompoundTag tag)||!tag.contains("location"))return Optional.empty();
            ResourceLocation id=new ResourceLocation(tag.getString("location"));
            Piece piece=new Piece(c.structureTemplateManager(),id,p.getPosition(),p.getRotation(),kind);
            chests+=piece.template().filterBlocks(BlockPos.ZERO,new StructurePlaceSettings(),Blocks.CHEST).size();
            if(id.getPath().contains("/core_"))cores++;
            if(id.getPath().contains("/level_"))hubs++;
            if(id.getPath().contains("/main_"))mains++;
            pieces.add(piece);
        }
        // Missing/blocked main routes never produce a dungeon with its reward room cut off.
        if(cores!=1||chests!=chestBudget||hubs!=3||mains!=(kind.equals("canopy")?6:kind.equals("hollow")?8:9))return Optional.empty();
        Piece entrance=pieces.get(0);BlockPos anchor=null;
        for(var marker:entrance.markers())if(marker.nbt()!=null&&marker.nbt().getString("metadata").equals("anchor")){anchor=marker.pos();break;}
        if(anchor==null)return Optional.empty();
        BlockPos shift=site.subtract(anchor);
        for(var p:pieces)p.move(shift.getX(),shift.getY(),shift.getZ());
        if(kind.equals("abyss")){
            int highest=-64,lowest=320,bottom=320,top=-64;
            for(var p:pieces){
                var b=p.getBoundingBox();bottom=Math.min(bottom,b.minY());top=Math.max(top,b.maxY());
                for(int x:bSamples(b.minX(),b.maxX()))for(int z:bSamples(b.minZ(),b.maxZ())){
                    int floor=f.floorAt(x,z);highest=Math.max(highest,floor);lowest=Math.min(lowest,floor);
                }
            }
            // Entire sealed complex above its highest sampled seabed, with finite piles down to lower terrain.
            int dy=highest+1-bottom;
            if(highest-lowest>34||top+dy>f.seaLevel-6)return Optional.empty();
            for(var p:pieces)p.move(0,dy,0);
            site=site.above(dy);
        }
        int anchors=0;
        for(var p:pieces){
            var b=p.getBoundingBox();
            if(b.minY()<WorldTreeField.MIN_Y+1||b.maxY()>WorldTreeField.MAX_Y)return Optional.empty();
            // Vanilla references look eight chunks around a target chunk. Never exceed that reach.
            if((b.minX()>>4)<c.chunkPos().x-8||(b.maxX()>>4)>c.chunkPos().x+8||(b.minZ()>>4)<c.chunkPos().z-8||(b.maxZ()>>4)>c.chunkPos().z+8)return Optional.empty();
            if(kind.equals("hollow")&&!p.templateId().endsWith("/entrance")){
                for(int x:bSamples(b.minX()+1,b.maxX()-1))for(int z:bSamples(b.minZ()+1,b.maxZ()-1))
                    if(f.floorAt(x,z)<b.maxY()+2)return Optional.empty();
            }
            if(!kind.equals("hollow")){
                int samples=0,obstructed=0;
                for(int x:bSamples(b.minX()+2,b.maxX()-2))for(int z:bSamples(b.minZ()+2,b.maxZ()-2)){
                    var col=f.column(x,z);
                    for(int y=b.minY()+4;y<=Math.min(b.maxY(),b.minY()+10);y+=3){samples++;if(col.woodAt(y))obstructed++;}
                }
                if(obstructed>samples/3)return Optional.empty();
            }
            p.planSupports(f);
            if(!p.planAccess(f))return Optional.empty();
            if(p.targets.size()>=2)anchors++;
        }
        Map<BlockPos,Port> sockets=new HashMap<>();
        List<Set<Integer>> graph=new ArrayList<>();for(int i=0;i<pieces.size();i++)graph.add(new HashSet<>());
        for(int i=0;i<pieces.size();i++)for(var socket:pieces.get(i).ports())sockets.put(socket.pos(),new Port(i,socket));
        for(var entry:sockets.values()){
            Direction facing=JigsawBlock.getFrontFacing(entry.info.state());
            var neighbor=sockets.get(entry.info.pos().relative(facing));
            if(neighbor!=null&&neighbor.index!=entry.index&&JigsawBlock.getFrontFacing(neighbor.info.state())==facing.getOpposite()){
                graph.get(entry.index).add(neighbor.index);pieces.get(entry.index).open.add(entry.info.pos());
            }
        }
        Set<Integer> reached=new HashSet<>();Deque<Integer> todo=new ArrayDeque<>();todo.add(0);
        while(!todo.isEmpty()){int i=todo.removeFirst();if(reached.add(i))todo.addAll(graph.get(i));}
        if(reached.size()!=pieces.size())return Optional.empty();
        if(kind.equals("canopy")){
            if(anchors<4)return Optional.empty();
            // A continuous braced deck may span short gaps; long chains of unsupported houses are rejected.
            int[] distance=new int[pieces.size()];Arrays.fill(distance,1000);todo.clear();
            for(int i=0;i<pieces.size();i++)if(pieces.get(i).targets.size()>=2){distance[i]=0;todo.add(i);}
            while(!todo.isEmpty()){int a=todo.removeFirst();for(int b:graph.get(a))if(distance[b]>distance[a]+1){distance[b]=distance[a]+1;todo.add(b);}}
            if(Arrays.stream(distance).max().orElse(1000)>3)return Optional.empty();
        }
        // Upper-floor piles/chains must end at intervening architecture, never cut through rooms below.
        clipSupportsToArchitecture(pieces);
        for(var p:pieces)p.extendBounds();
        return Optional.of(new GenerationStub(site,b->pieces.forEach(b::addPiece)));
    }
    private record SupportSurface(Piece owner,int y){}
    private static long columnKey(BlockPos p){return ChunkPos.asLong(p.getX(),p.getZ());}
    private static void clipSupportsToArchitecture(List<Piece> pieces){
        Set<Long> needed=new HashSet<>();
        for(var p:pieces)for(var at:p.targets.keySet())needed.add(columnKey(at));
        if(needed.isEmpty())return;
        Map<Long,List<SupportSurface>> surfaces=new HashMap<>();
        Map<StructureTemplate,CompoundTag> serialized=new IdentityHashMap<>();
        for(var p:pieces){
            var tag=serialized.computeIfAbsent(p.template(),t->t.save(new CompoundTag()));
            var palette=tag.getList("palette",10);boolean[] solid=new boolean[palette.size()];
            for(int i=0;i<solid.length;i++){
                var state=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),palette.getCompound(i));
                solid[i]=!state.is(Blocks.STRUCTURE_BLOCK)&&!state.is(Blocks.JIGSAW)&&state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE,BlockPos.ZERO);
            }
            for(Tag raw:tag.getList("blocks",10)){
                var block=(CompoundTag)raw;if(!solid[block.getInt("state")])continue;
                var pos=block.getList("pos",3);var local=new BlockPos(pos.getInt(0),pos.getInt(1),pos.getInt(2));
                var at=StructureTemplate.transform(local,Mirror.NONE,p.getRotation(),BlockPos.ZERO).offset(p.templatePosition());
                long key=columnKey(at);if(needed.contains(key))surfaces.computeIfAbsent(key,k->new ArrayList<>()).add(new SupportSurface(p,at.getY()));
            }
        }
        for(var p:pieces)for(var e:p.targets.entrySet()){
            int start=e.getKey().getY(),target=e.getValue();boolean up=target>start;
            for(var surface:surfaces.getOrDefault(columnKey(e.getKey()),List.of()))if(surface.owner!=p){
                int y=surface.y;
                if(up?y>start&&y<target:y<start&&y>target)target=y;
            }
            e.setValue(target);
        }
    }
    private static int[] bSamples(int min,int max){return new int[]{min,(min+max)/2,max};}
    private record Port(int index,StructureTemplate.StructureBlockInfo info){}
    private static StructurePlaceSettings settings(Rotation rotation){
        return new StructurePlaceSettings().setRotation(rotation).setIgnoreEntities(true).setKeepLiquids(false).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
    }

    public static final class Piece extends TemplateStructurePiece {
        private final String kind;
        private final Set<BlockPos> open=new HashSet<>();
        private final Map<BlockPos,Integer> targets=new HashMap<>();
        private final List<BlockPos> access=new ArrayList<>();
        private final Map<BlockPos,Integer> accessPiles=new HashMap<>();
        private Direction accessFacing=Direction.NORTH;
        private int accessHalfWidth=1;
        public Piece(StructureTemplateManager manager,ResourceLocation id,BlockPos pos,Rotation rotation,String kind){
            super(SaturnRingWorldGenRegistry.WORLD_TREE_DUNGEON_PIECE.get(),0,manager,id,id.toString(),settings(rotation),pos);this.kind=kind;
        }
        public Piece(StructurePieceSerializationContext c,CompoundTag tag){
            super(SaturnRingWorldGenRegistry.WORLD_TREE_DUNGEON_PIECE.get(),tag,c.structureTemplateManager(),id->settings(Rotation.valueOf(tag.getString("Rot"))));
            kind=tag.getString("Kind");for(long pos:tag.getLongArray("Open"))open.add(BlockPos.of(pos));
            for(Tag raw:tag.getList("Supports",10)){var value=(CompoundTag)raw;targets.put(BlockPos.of(value.getLong("Pos")),value.getInt("Y"));}
            for(long pos:tag.getLongArray("Access"))access.add(BlockPos.of(pos));
            accessFacing=Direction.from3DDataValue(tag.getInt("AccessFacing"));accessHalfWidth=tag.contains("AccessHalfWidth")?tag.getInt("AccessHalfWidth"):1;
            for(Tag raw:tag.getList("AccessPiles",10)){var value=(CompoundTag)raw;accessPiles.put(BlockPos.of(value.getLong("Pos")),value.getInt("Y"));}
            extendBounds();
        }
        public String templateId(){return templateName;}
        public String kind(){return kind;}
        public Set<BlockPos> openPorts(){return Collections.unmodifiableSet(open);}
        public Map<BlockPos,Integer> supportTargets(){return Collections.unmodifiableMap(targets);}
        public List<BlockPos> accessDeck(){return Collections.unmodifiableList(access);}
        public Map<BlockPos,Integer> accessPiles(){return Collections.unmodifiableMap(accessPiles);}
        public Direction accessFacing(){return accessFacing;}
        public int accessHalfWidth(){return accessHalfWidth;}
        public List<StructureTemplate.StructureBlockInfo> ports(){return template.filterBlocks(templatePosition,placeSettings.copy().setBoundingBox(null),Blocks.JIGSAW);}
        public List<StructureTemplate.StructureBlockInfo> markers(){return template.filterBlocks(templatePosition,placeSettings.copy().setBoundingBox(null),Blocks.STRUCTURE_BLOCK);}
        private void planSupports(WorldTreeField f){
            for(var marker:markers()){
                if(marker.nbt()==null)continue;String label=marker.nbt().getString("metadata");var p=marker.pos();
                if(kind.equals("abyss")&&label.equals("foot")){
                    int target=f.floorAt(p.getX(),p.getZ());if(p.getY()-target<=48)targets.put(p,target);
                }else if(kind.equals("canopy")){
                    var col=f.column(p.getX(),p.getZ());
                    if(label.equals("foot"))for(int y=p.getY()-1;y>=Math.max(100,p.getY()-72);y--)if(col.woodAt(y)){targets.put(p,y);break;}
                    if(label.equals("hang"))for(int y=p.getY()+1;y<=Math.min(WorldTreeField.MAX_Y,p.getY()+36);y++)if(col.woodAt(y)){targets.put(p,y);break;}
                }
            }
        }
        private void extendBounds(){
            var b=template.getBoundingBox(placeSettings,templatePosition);int min=b.minY(),max=b.maxY();
            for(int y:targets.values()){min=Math.min(min,y);max=Math.max(max,y);}
            for(int y:accessPiles.values())min=Math.min(min,y);
            for(var p:access){min=Math.min(min,p.getY());max=Math.max(max,p.getY()+4);}
            boundingBox=new BoundingBox(b.minX(),min,b.minZ(),b.maxX(),max,b.maxZ());
        }
        private boolean planAccess(WorldTreeField f){
            for(var marker:markers()){
                String label=marker.nbt()==null?"":marker.nbt().getString("metadata");
                if(!label.startsWith("access_"))continue;
                accessFacing=getRotation().rotate(label.equals("access_north")?Direction.NORTH:Direction.SOUTH);
                accessHalfWidth=kind.equals("canopy")?1:2;
                int maximum=kind.equals("canopy")?9:8;
                boolean connected=false;
                for(int count=maximum;count>=4;count--){
                    access.clear();accessPiles.clear();
                    if(planAccessLength(f,marker.pos().below(),count)){connected=true;break;}
                }
                if(!connected){access.clear();accessPiles.clear();return false;}
            }
            return true;
        }
        private boolean planAccessLength(WorldTreeField f,BlockPos start,int count){
            var end=start.relative(accessFacing,count-1);Direction side=accessFacing.getClockWise();int high=-64,low=320;
            for(int dx=-accessHalfWidth;dx<=accessHalfWidth;dx++){
                var q=end.relative(side,dx);var col=f.column(q.getX(),q.getZ());int ground=col.floor;
                if(kind.equals("canopy")){
                    ground=-1000;for(int y=start.getY()+2;y>=start.getY()-14;y--)if(col.woodAt(y)){ground=y;break;}
                    if(ground==-1000)return false;
                }
                high=Math.max(high,ground);low=Math.min(low,ground);
            }
            // Finish flush with the support surface; an extra block would create an unwalkable curb.
            int finish=high;
            if(high-low>2||Math.abs(finish-start.getY())>count-2)return false;
            for(int i=0;i<count;i++){
                int y=start.getY()+(int)Math.round((finish-start.getY())*i/(double)(count-1));
                var center=new BlockPos(start.getX(),y,start.getZ()).relative(accessFacing,i);access.add(center);
                for(int dx=-accessHalfWidth-1;dx<=accessHalfWidth+1;dx++){
                    var q=center.relative(side,dx);var col=f.column(q.getX(),q.getZ());
                    for(int head=1;head<=4;head++)if(col.woodAt(y+head))return false;
                    int target=col.floor;
                    if(kind.equals("canopy")){
                        target=-1000;for(int below=y;below>=y-16;below--)if(col.woodAt(below)){target=below;break;}
                    }
                    if(target>-1000&&y-target<=16&&target<=y)accessPiles.put(q,target);
                }
            }
            return true;
        }
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext c,CompoundTag tag){
            super.addAdditionalSaveData(c,tag);tag.putString("Rot",getRotation().name());tag.putString("Kind",kind);
            tag.putLongArray("Open",open.stream().mapToLong(BlockPos::asLong).sorted().toArray());
            var list=new ListTag();targets.entrySet().stream().sorted(Comparator.comparingLong(e->e.getKey().asLong())).forEach(e->{var t=new CompoundTag();t.putLong("Pos",e.getKey().asLong());t.putInt("Y",e.getValue());list.add(t);});tag.put("Supports",list);
            tag.putLongArray("Access",access.stream().mapToLong(BlockPos::asLong).toArray());tag.putInt("AccessFacing",accessFacing.get3DDataValue());tag.putInt("AccessHalfWidth",accessHalfWidth);
            var piles=new ListTag();accessPiles.forEach((p,y)->{var t=new CompoundTag();t.putLong("Pos",p.asLong());t.putInt("Y",y);piles.add(t);});tag.put("AccessPiles",piles);
        }
        @Override public void postProcess(WorldGenLevel level,StructureManager structures,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot){
            super.postProcess(level,structures,generator,random,clip,chunk,pivot);
            for(var port:ports())if(!open.contains(port.pos())){
                if(JigsawBlock.getFrontFacing(port.state()).getAxis()==Direction.Axis.Y){
                    // Vertical sockets are alignment anchors inside the central stair tower.
                    if(clip.isInside(port.pos()))level.setBlock(port.pos(),kind.equals("abyss")?BuiltInRegistries.BLOCK.get(new ResourceLocation("quark:iron_plate")).defaultBlockState():Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),2);
                    continue;
                }
                Direction across=JigsawBlock.getFrontFacing(port.state()).getClockWise();
                BlockState wall=sealState(across);
                for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<(kind.equals("canopy")?1:3);dy++){
                    var p=port.pos().relative(across,dx).above(dy);if(clip.isInside(p))level.setBlock(p,wall,2);
                }
            }
            placeAccess(level,clip);
            extendBounds();
        }
        private void placeAccess(WorldGenLevel level,BoundingBox clip){
            Direction across=accessFacing.getClockWise();
            BlockState deck=kind.equals("canopy")?BuiltInRegistries.BLOCK.get(new ResourceLocation("integrateddynamics:menril_planks")).defaultBlockState():Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            BlockState stairs=kind.equals("canopy")?Blocks.SPRUCE_STAIRS.defaultBlockState():Blocks.STONE_BRICK_STAIRS.defaultBlockState();
            BlockState filler=kind.equals("canopy")?BuiltInRegistries.BLOCK.get(new ResourceLocation("integrateddynamics:menril_log")).defaultBlockState():Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            for(int i=0;i<access.size();i++){
                var center=access.get(i);Direction uphill=null;
                if(i>0&&access.get(i-1).getY()<center.getY())uphill=accessFacing;
                else if(i+1<access.size()&&access.get(i+1).getY()<center.getY())uphill=accessFacing.getOpposite();
                for(int dx=-accessHalfWidth-1;dx<=accessHalfWidth+1;dx++){
                    var p=center.relative(across,dx);boolean edge=Math.abs(dx)>accessHalfWidth;
                    for(int h=1;h<=4;h++)if(clip.isInside(p.above(h)))level.setBlock(p.above(h),Blocks.AIR.defaultBlockState(),2);
                    Integer bottom=accessPiles.get(p);
                    if(bottom!=null)for(int y=p.getY()-1;y>bottom;y--){var q=new BlockPos(p.getX(),y,p.getZ());if(clip.isInside(q))level.setBlock(q,filler,2);}
                    if(clip.isInside(p))level.setBlock(p,uphill!=null&&!edge?stairs.setValue(StairBlock.FACING,uphill):deck,2);
                    if(edge&&i<access.size()-1&&clip.isInside(p.above())){
                        // Continuous timber/stone cheeks share a full face across each height change.
                        level.setBlock(p.above(),uphill==null?filler:stairs.setValue(StairBlock.FACING,uphill),2);
                    }
                }
            }
        }
        private BlockState sealState(Direction across){
            if(kind.equals("canopy")){
                var state=Blocks.SPRUCE_FENCE.defaultBlockState();
                if(across.getAxis()==Direction.Axis.X)return state.setValue(FenceBlock.EAST,true).setValue(FenceBlock.WEST,true);
                return state.setValue(FenceBlock.NORTH,true).setValue(FenceBlock.SOUTH,true);
            }
            boolean iron=kind.equals("abyss")||templateName.contains("/core_")||templateName.contains("/main_5_");
            return iron?BuiltInRegistries.BLOCK.get(new ResourceLocation("quark:iron_plate")).defaultBlockState():Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        @Override protected void handleDataMarker(String label,BlockPos p,ServerLevelAccessor level,RandomSource random,BoundingBox clip){
            if(!clip.isInside(p))return;
            if(label.equals("anchor")||label.equals("entrance")||label.startsWith("access_")){level.setBlock(p,Blocks.AIR.defaultBlockState(),2);return;}
            if(!label.equals("foot")&&!label.equals("hang"))return;
            BlockState wood=BuiltInRegistries.BLOCK.get(new ResourceLocation("integrateddynamics:menril_log")).defaultBlockState();
            BlockState iron=BuiltInRegistries.BLOCK.get(new ResourceLocation("quark:iron_plate")).defaultBlockState();
            Integer target=targets.get(p);
            level.setBlock(p,kind.equals("abyss")?iron:wood,2);
            if(target==null)return;
            boolean up=target>p.getY();
            BlockState material=up?Blocks.CHAIN.defaultBlockState():kind.equals("abyss")?iron:wood;
            for(int y=p.getY()+(up?1:-1);up?y<target:y>target;y+=up?1:-1){
                var at=new BlockPos(p.getX(),y,p.getZ());if(clip.isInside(at))level.setBlock(at,material,2);
            }
        }
    }
}
