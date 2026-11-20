package com.tonywww.slashblade_sendims.client.compat;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.compat.lightspeed.LightspeedStartup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class LightspeedClientEvents {
    private LightspeedClientEvents() {}

    @SubscribeEvent
    public static void loggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        LightspeedStartup.finishSkippedTitleScreen();
    }
}
