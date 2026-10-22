package com.tonywww.slashblade_sendims.network;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.client.SukunaVisualClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.UUID;
import java.util.function.Supplier;

/** Compact server phase/lease descriptor, with no per-slash entities or particle packets. */
public record SukunaVisualPacket(ResourceLocation dimension,UUID owner,Stage stage,long id,
        Vec3 origin,Vec3 forward,int age) {
    public enum Stage {DOMAIN,END,SPACE,WHEEL}
    public boolean valid() {
        return finite(origin) && finite(forward) && forward.lengthSqr()>.9 && forward.lengthSqr()<1.1
                && Math.abs(forward.y)<.001 && age>=0 && age<=150;
    }
    private static boolean finite(Vec3 p) {
        return Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z);
    }
    public static void encode(SukunaVisualPacket p,FriendlyByteBuf b) {
        b.writeResourceLocation(p.dimension);b.writeUUID(p.owner);b.writeEnum(p.stage);b.writeLong(p.id);
        b.writeDouble(p.origin.x);b.writeDouble(p.origin.y);b.writeDouble(p.origin.z);
        b.writeDouble(p.forward.x);b.writeDouble(p.forward.y);b.writeDouble(p.forward.z);b.writeVarInt(p.age);
    }
    public static SukunaVisualPacket decode(FriendlyByteBuf b) {
        return new SukunaVisualPacket(b.readResourceLocation(),b.readUUID(),b.readEnum(Stage.class),b.readLong(),
                new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
                new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readVarInt());
    }
    public static void handle(SukunaVisualPacket p,Supplier<NetworkEvent.Context> supplier) {
        var context=supplier.get();
        if(p.valid())context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->SukunaVisualClient.accept(p)));
        context.setPacketHandled(true);
    }
    public static void send(LivingEntity user,Stage stage,long id,Vec3 origin,Vec3 forward,int age) {
        if(user.level() instanceof ServerLevel level)send(level,user,stage,id,origin,forward,age);
    }
    public static void send(ServerLevel level,LivingEntity user,Stage stage,long id,Vec3 origin,Vec3 forward,int age) {
        var p=new SukunaVisualPacket(level.dimension().location(),user.getUUID(),stage,id,origin,forward,age);
        for(var observer:level.players())if(observer.position().distanceToSqr(origin)<=128*128)
            SenDims.NETWORK.send(PacketDistributor.PLAYER.with(()->observer),p);
    }
}
