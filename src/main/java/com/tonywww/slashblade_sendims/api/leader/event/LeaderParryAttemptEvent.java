package com.tonywww.slashblade_sendims.api.leader.event;

import com.tonywww.slashblade_sendims.api.leader.LeaderParryDecision;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;
import java.util.Objects;

/** Posted after a parry is validated but before its state transition is committed. */
public class LeaderParryAttemptEvent extends Event {
    private final LivingEntity target;
    @Nullable
    private final LivingEntity actor;
    private final ResourceLocation sourceId;
    private final LeaderSnapshot oldSnapshot;
    private LeaderParryDecision decision = LeaderParryDecision.PARRY;
    private int parriedTicks;
    private int stunTicks;

    public LeaderParryAttemptEvent(LivingEntity target, @Nullable LivingEntity actor,
                                   ResourceLocation sourceId, LeaderSnapshot oldSnapshot,
                                   int parriedTicks, int stunTicks) {
        this.target = Objects.requireNonNull(target, "target");
        this.actor = actor;
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
        this.oldSnapshot = Objects.requireNonNull(oldSnapshot, "oldSnapshot");
        this.parriedTicks = requirePositive(parriedTicks, "parriedTicks");
        this.stunTicks = requirePositive(stunTicks, "stunTicks");
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

    public LeaderSnapshot getOldSnapshot() {
        return oldSnapshot;
    }

    public LeaderParryDecision getDecision() {
        return decision;
    }

    public void setDecision(LeaderParryDecision decision) {
        this.decision = Objects.requireNonNull(decision, "decision");
    }

    public int getParriedTicks() {
        return parriedTicks;
    }

    public void setParriedTicks(int parriedTicks) {
        this.parriedTicks = requirePositive(parriedTicks, "parriedTicks");
    }

    public int getStunTicks() {
        return stunTicks;
    }

    public void setStunTicks(int stunTicks) {
        this.stunTicks = requirePositive(stunTicks, "stunTicks");
    }

    private static int requirePositive(int ticks, String name) {
        if (ticks <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return ticks;
    }
}