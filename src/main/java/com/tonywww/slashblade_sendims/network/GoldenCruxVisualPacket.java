package com.tonywww.slashblade_sendims.network;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.client.GoldenCruxVisualClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** One small descriptor per phase; the entire crown is drawn locally. */
public record GoldenCruxVisualPacket(ResourceLocation dimension, int entityId, long castId,
        Stage stage, Vec3 origin, Vec3 direction) {
    public enum Stage { LEAP, DIVE, STAB, BURST, CANCEL }

    public boolean valid() {
        return entityId >= 0 && finite(origin) && finite(direction) && direction.lengthSqr() < 16;
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    public static void encode(GoldenCruxVisualPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension); buffer.writeVarInt(packet.entityId);
        buffer.writeLong(packet.castId); buffer.writeEnum(packet.stage);
        buffer.writeDouble(packet.origin.x); buffer.writeDouble(packet.origin.y); buffer.writeDouble(packet.origin.z);
        buffer.writeFloat((float) packet.direction.x); buffer.writeFloat((float) packet.direction.y);
        buffer.writeFloat((float) packet.direction.z);
    }

    public static GoldenCruxVisualPacket decode(FriendlyByteBuf buffer) {
        return new GoldenCruxVisualPacket(buffer.readResourceLocation(), buffer.readVarInt(),
                buffer.readLong(), buffer.readEnum(Stage.class),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));
    }

    public static void handle(GoldenCruxVisualPacket packet, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        if (packet.valid()) context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> GoldenCruxVisualClient.accept(packet)));
        context.setPacketHandled(true);
    }

    public static void send(LivingEntity user, long castId, Stage stage, Vec3 origin, Vec3 direction) {
        if (!(user.level() instanceof ServerLevel level)) return;
        var packet = new GoldenCruxVisualPacket(level.dimension().location(), user.getId(), castId, stage, origin, direction);
        for (var observer : level.players()) if (observer.position().distanceToSqr(origin) <= 96 * 96)
            SenDims.NETWORK.send(PacketDistributor.PLAYER.with(() -> observer), packet);
    }
}
