package com.tonywww.slashblade_sendims.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class SuperSlashArtsReleaseEvent extends Event {
    private final ServerPlayer player;

    public SuperSlashArtsReleaseEvent(ServerPlayer player) {
        this.player = player;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}

