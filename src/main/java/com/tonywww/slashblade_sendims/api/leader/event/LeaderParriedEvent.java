package com.tonywww.slashblade_sendims.api.leader.event;

import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;
import java.util.Objects;

public class LeaderParriedEvent extends Event {
    private final LivingEntity target;
    @Nullable
    private final LivingEntity actor;
    private final ResourceLocation sourceId;
    private final LeaderSnapshot snapshot;

    public LeaderParriedEvent(LivingEntity target, @Nullable LivingEntity actor,
                              ResourceLocation sourceId, LeaderSnapshot snapshot) {
        this.target = Objects.requireNonNull(target, "target");
        this.actor = actor;
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
    }

    public LivingEntity getTarget() {
        return target;
    }

    @Nullable
    public LivingEntity getActor() {
        return actor;
    }

    public ResourceLocation getSourceId() {
        return sourceId;
    }

    public LeaderSnapshot getSnapshot() {
        return snapshot;
    }
}