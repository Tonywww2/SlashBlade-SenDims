package com.tonywww.slashblade_sendims.kubejs;

import com.tonywww.slashblade_sendims.kubejs.events.SBSDEvents;
import com.tonywww.slashblade_sendims.kubejs.events.TierRegisterEventJS;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import net.minecraftforge.eventbus.api.IEventBus;

public class SBSDPlugin extends KubeJSPlugin {

    public static void register(IEventBus bus) {
        bus.addListener(TierRegisterEventJS::tierRegisterHandler);
    }

    @Override
    public void registerEvents() {
        SBSDEvents.GROUP.register();
    }
}
