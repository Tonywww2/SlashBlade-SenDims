package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.network.LeaderStateSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;

final class LeaderStateSynchronizer {
    private LeaderStateSynchronizer() {
    }

    static void sync(LivingEntity entity) {
        LeaderStateStorage.getSnapshot(entity).ifPresent(snapshot ->
                SenDims.NETWORK.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                        createPacket(entity, snapshot)
                ));
    }

    static void syncTo(ServerPlayer player, LivingEntity entity) {
        LeaderStateStorage.getSnapshot(entity).ifPresent(snapshot ->
                SenDims.NETWORK.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        createPacket(entity, snapshot)
                ));
    }

    private static LeaderStateSyncPacket createPacket(LivingEntity entity, LeaderSnapshot snapshot) {
        return new LeaderStateSyncPacket(
                entity.getId(),
                entity.getUUID(),
                snapshot,
                entity.level().getGameTime()
        );
    }
}