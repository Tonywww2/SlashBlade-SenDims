package com.tonywww.slashblade_sendims.kubejs.events;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface SBSDEvents {
    EventGroup GROUP = EventGroup.of("SBSDEvents");
    EventHandler TierRegister = GROUP.startup("registerTier", () -> TierRegisterEventJS.class);
}
