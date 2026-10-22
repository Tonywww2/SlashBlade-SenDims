package com.tonywww.slashblade_sendims.yakuman;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.client.YakumanVisualClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import java.util.*;
import java.util.function.Supplier;

/** One batched semantic update per observer/tick; geometry and particles are generated locally. */
public record YakumanVisualPacket(ResourceLocation dimension,List<Visual> visuals) {
    public enum Shape { END,SLASH,NEEDLE,PHANTOM,TILE,STACK,FALL_STACK,ORBIT,GATE,RING,DRAGON,SHADOW,GOLD,FACING_TILE }
    public record Visual(long id,Shape shape,Vec3 pos,Vec3 motion,int life,float scale,int color,int tile) {
        boolean valid(){return finite(pos)&&finite(motion)&&motion.lengthSqr()<100&&life>=0&&life<=200
                &&Float.isFinite(scale)&&scale>0&&scale<=16&&tile>=0&&tile<35;}
    }
    private static final Map<ServerLevel,List<Visual>> QUEUED=new IdentityHashMap<>();
    private static boolean finite(Vec3 p){return Double.isFinite(p.x)&&Double.isFinite(p.y)&&Double.isFinite(p.z);}
    public static void emit(ServerLevel level,long id,Shape shape,Vec3 pos,Vec3 motion,int life,float scale,int rgb,int tile) {
        QUEUED.computeIfAbsent(level,l->new ArrayList<>()).add(new Visual(id,shape,pos,motion,life,scale,rgb,tile));
    }
    public static void flush() {
        for(var entry:QUEUED.entrySet()) {
            var level=entry.getKey();
            for(var player:level.players()) {
                List<Visual> visible=new ArrayList<>();
                for(var v:entry.getValue())if(player.position().distanceToSqr(v.pos)<96*96)visible.add(v);
                for(int i=0;i<visible.size();i+=256) {
                    var packet=new YakumanVisualPacket(level.dimension().location(),List.copyOf(visible.subList(i,Math.min(i+256,visible.size()))));
                    SenDims.NETWORK.send(PacketDistributor.PLAYER.with(()->player),packet);
                }
            }
        }
        QUEUED.clear();
    }
    public static void clear(){QUEUED.clear();}
    public static void encode(YakumanVisualPacket p,FriendlyByteBuf b) {
        b.writeResourceLocation(p.dimension);b.writeVarInt(p.visuals.size());
        for(var v:p.visuals) {
            b.writeLong(v.id);b.writeEnum(v.shape);
            b.writeDouble(v.pos.x);b.writeDouble(v.pos.y);b.writeDouble(v.pos.z);
            b.writeFloat((float)v.motion.x);b.writeFloat((float)v.motion.y);b.writeFloat((float)v.motion.z);
            b.writeVarInt(v.life);b.writeFloat(v.scale);b.writeInt(v.color);b.writeByte(v.tile);
        }
    }
    public static YakumanVisualPacket decode(FriendlyByteBuf b) {
        var dim=b.readResourceLocation();int n=b.readVarInt();
        if(n<0||n>256)throw new IllegalArgumentException("Yakuman visual batch exceeds limit");
        List<Visual> result=new ArrayList<>(n);
        for(int i=0;i<n;i++) {
            var v=new Visual(b.readLong(),b.readEnum(Shape.class),
                    new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
                    new Vec3(b.readFloat(),b.readFloat(),b.readFloat()),
                    b.readVarInt(),b.readFloat(),b.readInt(),b.readUnsignedByte());
            if(v.valid())result.add(v);
        }
        return new YakumanVisualPacket(dim,List.copyOf(result));
    }
    public static void handle(YakumanVisualPacket packet,Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->YakumanVisualClient.accept(packet)));
        context.setPacketHandled(true);
    }
}
