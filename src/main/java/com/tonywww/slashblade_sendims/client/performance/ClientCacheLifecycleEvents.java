package com.tonywww.slashblade_sendims.client.performance;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class ClientCacheLifecycleEvents {
    private ClientCacheLifecycleEvents() {}

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) MinimapFilterState.clear();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        RenderResourceEpoch.invalidate();
        QuestHudCaches.clear();
        MinimapFilterState.clear();
    }
}
