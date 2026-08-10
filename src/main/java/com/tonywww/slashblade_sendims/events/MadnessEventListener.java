package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.MadnessSyncPacket;
import com.tonywww.slashblade_sendims.se.FrenziedFlame;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class MadnessEventListener {
    private MadnessEventListener() {
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)
                || !(event.getTarget() instanceof LivingEntity livingEntity)) {
            return;
        }

        syncTo(serverPlayer, livingEntity);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncTo(serverPlayer, serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncTo(serverPlayer, serverPlayer);
        }
    }

    private static void syncTo(ServerPlayer serverPlayer, LivingEntity livingEntity) {
        int madness = livingEntity.getPersistentData().getInt(FrenziedFlame.MADNESS_PATH);
        if (madness != 0) {
            SenDims.NETWORK.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new MadnessSyncPacket(livingEntity.getId(), madness));
        }
    }
}