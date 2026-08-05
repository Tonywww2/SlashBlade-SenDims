package com.tonywww.slashblade_sendims.network;

import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.client.leader.ClientLeaderStateCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Supplier;

public record LeaderStateSyncPacket(int entityId, UUID entityUuid, LeaderProfile profile,
                                    LeaderPhase phase, int remainingTicks, long serverGameTime) {
    public LeaderStateSyncPacket(int entityId, UUID entityUuid, LeaderSnapshot snapshot,
                                 long serverGameTime) {
        this(entityId, entityUuid, snapshot.profile(), snapshot.phase(),
                snapshot.remainingTicks().orElse(-1), serverGameTime);
    }

    public LeaderSnapshot snapshot() {
        OptionalInt remaining = remainingTicks >= 0
                ? OptionalInt.of(remainingTicks)
                : OptionalInt.empty();
        return new LeaderSnapshot(profile, phase, remaining);
    }

    public static void encode(LeaderStateSyncPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeUUID(message.entityUuid);
        buffer.writeEnum(message.profile);
        buffer.writeEnum(message.phase);
        buffer.writeVarInt(message.remainingTicks + 1);
        buffer.writeLong(message.serverGameTime);
    }

    public static LeaderStateSyncPacket decode(FriendlyByteBuf buffer) {
        return new LeaderStateSyncPacket(
                buffer.readVarInt(),
                buffer.readUUID(),
                buffer.readEnum(LeaderProfile.class),
                buffer.readEnum(LeaderPhase.class),
                buffer.readVarInt() - 1,
                buffer.readLong()
        );
    }

    public static void handle(LeaderStateSyncPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientLeaderStateCache.accept(message)
        ));
        context.setPacketHandled(true);
    }
}