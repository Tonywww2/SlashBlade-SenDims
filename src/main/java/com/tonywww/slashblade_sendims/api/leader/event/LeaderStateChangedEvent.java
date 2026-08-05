package com.tonywww.slashblade_sendims.api.leader.event;

import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.api.leader.LeaderStateChangeCause;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;

public class LeaderStateChangedEvent extends Event {
    private final LivingEntity entity;
    private final LeaderSnapshot oldSnapshot;
    private final LeaderSnapshot newSnapshot;
    private final LeaderStateChangeCause cause;

    public LeaderStateChangedEvent(LivingEntity entity, LeaderSnapshot oldSnapshot,
                                   LeaderSnapshot newSnapshot, LeaderStateChangeCause cause) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.oldSnapshot = Objects.requireNonNull(oldSnapshot, "oldSnapshot");
        this.newSnapshot = Objects.requireNonNull(newSnapshot, "newSnapshot");
        this.cause = Objects.requireNonNull(cause, "cause");
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public LeaderSnapshot getOldSnapshot() {
        return oldSnapshot;
    }

    public LeaderSnapshot getNewSnapshot() {
        return newSnapshot;
    }

    public LeaderStateChangeCause getCause() {
        return cause;
    }
}