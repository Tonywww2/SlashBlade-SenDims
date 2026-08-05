package com.tonywww.slashblade_sendims.client.leader;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.api.leader.event.ClientLeaderStateChangedEvent;
import com.tonywww.slashblade_sendims.leader.LeaderStateStorage;
import com.tonywww.slashblade_sendims.network.LeaderStateSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientLeaderStateCache {
    private static final Map<UUID, LeaderStateSyncPacket> PENDING = new HashMap<>();

    private ClientLeaderStateCache() {
    }

    public static void accept(LeaderStateSyncPacket message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            PENDING.put(message.entityUuid(), message);
            return;
        }

        Entity entity = level.getEntity(message.entityId());
        if (entity instanceof LivingEntity living && living.getUUID().equals(message.entityUuid())) {
            apply(living, message);
        } else {
            PENDING.put(message.entityUuid(), message);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        LeaderStateSyncPacket pending = PENDING.remove(living.getUUID());
        if (pending != null) {
            apply(living, pending);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            PENDING.clear();
        }
    }

    private static void apply(LivingEntity entity, LeaderStateSyncPacket message) {
        Optional<LeaderSnapshot> oldSnapshot = LeaderStateStorage.getSnapshot(entity);
        LeaderSnapshot packetSnapshot = message.snapshot();
        LeaderStateStorage.applyClientSnapshot(entity, packetSnapshot, message.serverGameTime());
        LeaderSnapshot newSnapshot = LeaderStateStorage.getSnapshot(entity).orElseThrow();
        if (!oldSnapshot.filter(newSnapshot::equals).isPresent()) {
            MinecraftForge.EVENT_BUS.post(new ClientLeaderStateChangedEvent(entity, oldSnapshot, newSnapshot));
        }
    }
}