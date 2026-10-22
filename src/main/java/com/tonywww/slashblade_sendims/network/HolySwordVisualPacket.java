package com.tonywww.slashblade_sendims.network;

import com.tonywww.slashblade_sendims.client.HolySwordVisualClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** One descriptor per animation transition, never one packet per mote/frame. */
public record HolySwordVisualPacket(ResourceLocation dimension, UUID user, int entityId,
        long castId, Stage stage, boolean excalibur, Vec3 origin, Vec3 end,
        float skyHeight, int chargeTicks) {
    public enum Stage { CHARGE, STOP_CHARGE, OPEN, FINISH, PULSE_0, PULSE_1, PULSE_2, CANCEL }

    public static void encode(HolySwordVisualPacket p, FriendlyByteBuf b) {
        b.writeResourceLocation(p.dimension); b.writeUUID(p.user); b.writeVarInt(p.entityId);
        b.writeLong(p.castId); b.writeEnum(p.stage); b.writeBoolean(p.excalibur);
        b.writeDouble(p.origin.x); b.writeDouble(p.origin.y); b.writeDouble(p.origin.z);
        b.writeDouble(p.end.x); b.writeDouble(p.end.y); b.writeDouble(p.end.z);
        b.writeFloat(p.skyHeight); b.writeVarInt(p.chargeTicks);
    }

    public static HolySwordVisualPacket decode(FriendlyByteBuf b) {
        return new HolySwordVisualPacket(b.readResourceLocation(), b.readUUID(), b.readVarInt(),
                b.readLong(), b.readEnum(Stage.class), b.readBoolean(),
                new Vec3(b.readDouble(), b.readDouble(), b.readDouble()),
                new Vec3(b.readDouble(), b.readDouble(), b.readDouble()), b.readFloat(), b.readVarInt());
    }

    public boolean valid() {
        return finite(origin) && finite(end) && origin.distanceToSqr(end) <= 1600
                && Float.isFinite(skyHeight) && skyHeight >= 0 && skyHeight <= 96
                && chargeTicks >= 0 && chargeTicks <= 72000;
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    public static void handle(HolySwordVisualPacket p, Supplier<NetworkEvent.Context> supplier) {
        var context = supplier.get();
        if (p.valid()) context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> HolySwordVisualClient.accept(p)));
        context.setPacketHandled(true);
    }
}
