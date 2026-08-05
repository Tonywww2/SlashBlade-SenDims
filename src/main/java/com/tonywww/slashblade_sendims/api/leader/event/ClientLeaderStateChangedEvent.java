package com.tonywww.slashblade_sendims.api.leader.event;

import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;
import java.util.Optional;

public class ClientLeaderStateChangedEvent extends Event {
    private final LivingEntity entity;
    private final Optional<LeaderSnapshot> oldSnapshot;
    private final LeaderSnapshot newSnapshot;

    public ClientLeaderStateChangedEvent(LivingEntity entity, Optional<LeaderSnapshot> oldSnapshot,
                                         LeaderSnapshot newSnapshot) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.oldSnapshot = Objects.requireNonNull(oldSnapshot, "oldSnapshot");
        this.newSnapshot = Objects.requireNonNull(newSnapshot, "newSnapshot");
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public Optional<LeaderSnapshot> getOldSnapshot() {
        return oldSnapshot;
    }

    public LeaderSnapshot getNewSnapshot() {
        return newSnapshot;
    }
}