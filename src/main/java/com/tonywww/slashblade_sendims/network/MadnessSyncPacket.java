package com.tonywww.slashblade_sendims.network;

import com.tonywww.slashblade_sendims.se.FrenziedFlame;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MadnessSyncPacket {
    private final int entityId;
    private final int madness;

    public MadnessSyncPacket(int entityId, int madness) {
        this.entityId = entityId;
        this.madness = madness;
    }

    public static void encode(MadnessSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.madness);
    }

    public static MadnessSyncPacket decode(FriendlyByteBuf buf) {
        return new MadnessSyncPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(MadnessSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player != null && player.getId() == msg.entityId) {
                player.getPersistentData().putInt(FrenziedFlame.MADNESS_PATH, msg.madness);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}