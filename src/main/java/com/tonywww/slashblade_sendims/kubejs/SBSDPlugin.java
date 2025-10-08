package com.tonywww.slashblade_sendims.kubejs;

import com.tonywww.slashblade_sendims.kubejs.events.TierRegisterEventJS;
import com.tonywww.slashblade_sendims.registeries.SBSDParticles;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import net.minecraftforge.eventbus.api.IEventBus;

public class SBSDPlugin extends KubeJSPlugin {

    public static void register(IEventBus bus) {
        bus.addListener(TierRegisterEventJS::tierRegisterHandler);
    }
}
