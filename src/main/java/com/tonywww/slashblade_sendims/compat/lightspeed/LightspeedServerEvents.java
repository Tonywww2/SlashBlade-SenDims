package com.tonywww.slashblade_sendims.compat.lightspeed;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class LightspeedServerEvents {
    private LightspeedServerEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        if (FMLEnvironment.dist.isDedicatedServer()) LightspeedStartup.finishOnDedicatedServer();
    }
}
