package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.overlay.MadnessOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetupEvents {

    @SubscribeEvent
    public static void registerGuiOverlay(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("slashblade_sendims.madness", MadnessOverlay.INSTANCE);

    }
}
