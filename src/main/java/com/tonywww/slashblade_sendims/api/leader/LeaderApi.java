package com.tonywww.slashblade_sendims.api.leader;

import com.tonywww.slashblade_sendims.leader.LeaderManager;
import com.tonywww.slashblade_sendims.leader.LeaderStateStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * Public entry point for registering, querying, and parrying Leader entities.
 * State-changing methods must be called on the logical server's main thread.
 */
public final class LeaderApi {
    private LeaderApi() {
    }

    /** Returns synchronized state on the client and authoritative state on the server. */
    public static boolean isLeader(LivingEntity entity) {
        return LeaderStateStorage.isLeader(Objects.requireNonNull(entity, "entity"));
    }

    /** Returns an empty value when the entity is not registered as a Leader. */
    public static Optional<LeaderSnapshot> getSnapshot(LivingEntity entity) {
        return LeaderStateStorage.getSnapshot(Objects.requireNonNull(entity, "entity"));
    }

    public static boolean isParryable(LivingEntity entity) {
        return getSnapshot(entity).map(snapshot -> snapshot.phase() == LeaderPhase.PARRYABLE).orElse(false);
    }

    public static boolean isParried(LivingEntity entity) {
        return getSnapshot(entity).map(snapshot -> snapshot.phase() == LeaderPhase.PARRIED).orElse(false);
    }

    /** Registers one server-side entity with the default {@link LeaderProfile#EXTERNAL} profile. */
    public static boolean registerLeader(LivingEntity entity) {
        return registerLeader(entity, LeaderProfile.EXTERNAL);
    }

    /**
     * Registers one server-side entity. Registration is idempotent; a conflicting explicit
     * profile is rejected and returns {@code false}.
     */
    public static boolean registerLeader(LivingEntity entity, LeaderProfile profile) {
        return LeaderManager.registerLeader(entity, profile);
    }

    /** Registers a default profile for future instances of an entity type. */
    public static boolean registerLeaderType(EntityType<? extends LivingEntity> entityType, LeaderProfile profile) {
        return LeaderManager.registerLeaderType(entityType, profile);
    }

    /** Opens an indefinite parry window for an EXTERNAL Leader on the logical server. */
    public static boolean openParryWindow(LivingEntity entity) {
        return LeaderManager.openParryWindow(entity);
    }

    /** Opens a timed parry window for an EXTERNAL Leader on the logical server. */
    public static boolean openParryWindow(LivingEntity entity, int durationTicks) {
        return LeaderManager.openParryWindow(entity, durationTicks);
    }

    /** Closes an EXTERNAL Leader's current parry window on the logical server. */
    public static boolean closeParryWindow(LivingEntity entity) {
        return LeaderManager.closeParryWindow(entity);
    }

    /**
     * Attempts an authoritative parry transition. On success this applies the standard target
     * reaction, synchronizes clients, and posts the server-side Leader events.
     */
    public static ParryResult tryParry(LivingEntity target, @Nullable LivingEntity actor,
                                       ResourceLocation sourceId) {
        return LeaderManager.tryParry(target, actor, sourceId);
    }

    /**
     * Forces an authoritative parried transition without requiring an open parry window.
     * The target must be a server-side Leader that is not already parried.
     */
    public static ParryResult enterParriedState(LivingEntity target, @Nullable LivingEntity actor,
                                                ResourceLocation sourceId, int parriedTicks,
                                                int stunTicks) {
        return LeaderManager.enterParriedState(
                target, actor, sourceId, parriedTicks, stunTicks);
    }
}