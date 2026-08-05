package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderParryDecision;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.api.leader.LeaderStateChangeCause;
import com.tonywww.slashblade_sendims.api.leader.ParryResult;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParryAbsorbedEvent;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParryAttemptEvent;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParriedEvent;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderStateChangedEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import twilightforest.entity.boss.Naga;

public final class LeaderManager {
    private static final Map<EntityType<?>, LeaderProfile> TYPE_PROFILES = new ConcurrentHashMap<>();
    private static final Set<UUID> ACTIVE_PARRY_TRANSITIONS = new HashSet<>();

    private LeaderManager() {
    }

    public static boolean registerLeader(LivingEntity entity, LeaderProfile profile) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(profile, "profile");
        if (entity.level().isClientSide) {
            return false;
        }
        if (LeaderStateStorage.hasExplicitProfile(entity)
                && LeaderStateStorage.getProfile(entity) != profile) {
            return false;
        }

        boolean needsInitialization = profile == LeaderProfile.MANAGED
                && !entity.getPersistentData().getBoolean(SBSDValues.IS_INITIALIZED);
        LeaderStateStorage.register(entity, profile);
        if (needsInitialization) {
            SBSDLeader.initializeLeader(entity, entity.getPersistentData());
        }
        LeaderStateSynchronizer.sync(entity);
        return true;
    }

    public static boolean registerLeaderType(EntityType<? extends LivingEntity> entityType,
                                             LeaderProfile profile) {
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(profile, "profile");
        LeaderProfile existing = TYPE_PROFILES.putIfAbsent(entityType, profile);
        return existing == null || existing == profile;
    }

    public static void applyRegistration(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        if (LeaderStateStorage.hasExplicitProfile(entity)) {
            if (LeaderStateStorage.getProfile(entity) == LeaderProfile.MANAGED) {
                SBSDLeader.initializeLeader(entity, entity.getPersistentData());
            }
            LeaderStateSynchronizer.sync(entity);
            return;
        }

        LeaderProfile typeProfile = TYPE_PROFILES.get(entity.getType());
        if (typeProfile != null) {
            registerLeader(entity, typeProfile);
        } else if (LeaderStateStorage.isLeader(entity)) {
            if (entity.getPersistentData().contains(SBSDValues.APOTH_BOSS)) {
                SBSDLeader.initializeLeader(entity, entity.getPersistentData());
            }
            LeaderStateSynchronizer.sync(entity);
        }
    }

    public static boolean openParryWindow(LivingEntity entity) {
        return openParryWindow(entity, OptionalInt.empty());
    }

    public static boolean openParryWindow(LivingEntity entity, int durationTicks) {
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("durationTicks must be positive");
        }
        return openParryWindow(entity, OptionalInt.of(durationTicks));
    }

    public static boolean closeParryWindow(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        if (isParryTransitionActive(entity)
                || !canControlExternalWindow(entity)
                || LeaderStateStorage.getPhase(entity) != LeaderPhase.PARRYABLE) {
            return false;
        }
        return transition(entity, LeaderPhase.NORMAL, LeaderStateChangeCause.WINDOW_CLOSED,
                LeaderStateStorage::clearExternalWindow);
    }

    public static ParryResult tryParry(LivingEntity target, @Nullable LivingEntity actor,
                                       ResourceLocation sourceId) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(sourceId, "sourceId");
        if (target.level().isClientSide) {
            return ParryResult.WRONG_SIDE;
        }
        if (!LeaderStateStorage.isLeader(target)) {
            return ParryResult.NOT_LEADER;
        }
        if (!isValidParryTarget(target)
                || LeaderStateStorage.getPhase(target) != LeaderPhase.PARRYABLE) {
            return ParryResult.NOT_PARRYABLE;
        }
        if (!ACTIVE_PARRY_TRANSITIONS.add(target.getUUID())) {
            return ParryResult.NOT_PARRYABLE;
        }

        try {
            LeaderSnapshot oldSnapshot = LeaderStateStorage.getSnapshot(target).orElseThrow();
            LeaderParryAttemptEvent attempt = new LeaderParryAttemptEvent(
                    target,
                    actor,
                    sourceId,
                    oldSnapshot,
                    SBSDValues.END_PARRIED_TICK,
                    LeaderCombatHandler.defaultStunTicks(target)
            );
            MinecraftForge.EVENT_BUS.post(attempt);

                if (!isValidParryTarget(target)
                    || !LeaderStateStorage.isLeader(target)
                    || LeaderStateStorage.getPhase(target) != LeaderPhase.PARRYABLE) {
                return ParryResult.NOT_PARRYABLE;
            }
            if (attempt.getDecision() == LeaderParryDecision.ABSORB) {
                return commitAbsorbed(target, actor, sourceId, oldSnapshot);
            }
            return commitParried(target, actor, sourceId, oldSnapshot,
                    attempt.getParriedTicks(), attempt.getStunTicks());
        } finally {
            ACTIVE_PARRY_TRANSITIONS.remove(target.getUUID());
        }
    }

    public static ParryResult enterParriedState(LivingEntity target, @Nullable LivingEntity actor,
                                                ResourceLocation sourceId, int parriedTicks,
                                                int stunTicks) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(sourceId, "sourceId");
        requirePositive(parriedTicks, "parriedTicks");
        requirePositive(stunTicks, "stunTicks");
        if (target.level().isClientSide) {
            return ParryResult.WRONG_SIDE;
        }
        if (!LeaderStateStorage.isLeader(target)) {
            return ParryResult.NOT_LEADER;
        }
        if (!isValidParryTarget(target)
            || LeaderStateStorage.getPhase(target) == LeaderPhase.PARRIED
                || !ACTIVE_PARRY_TRANSITIONS.add(target.getUUID())) {
            return ParryResult.NOT_PARRYABLE;
        }

        try {
            LeaderSnapshot oldSnapshot = LeaderStateStorage.getSnapshot(target).orElseThrow();
            return commitParried(target, actor, sourceId, oldSnapshot, parriedTicks, stunTicks);
        } finally {
            ACTIVE_PARRY_TRANSITIONS.remove(target.getUUID());
        }
    }

    public static void tickExternal(LivingEntity entity) {
        if (entity.level().isClientSide || !LeaderStateStorage.isLeader(entity)
                || LeaderStateStorage.getProfile(entity) != LeaderProfile.EXTERNAL) {
            return;
        }
        if (LeaderStateStorage.getPhase(entity) == LeaderPhase.PARRYABLE
                && LeaderStateStorage.hasExternalWindowExpired(entity)) {
            closeParryWindow(entity);
        }
    }

    public static boolean setManagedParryable(LivingEntity entity, boolean parryable) {
        if (entity.level().isClientSide || isParryTransitionActive(entity)
            || !LeaderStateStorage.isLeader(entity)
                || LeaderStateStorage.getProfile(entity) != LeaderProfile.MANAGED
                || LeaderStateStorage.getPhase(entity) == LeaderPhase.PARRIED) {
            return false;
        }
        LeaderPhase nextPhase = parryable ? LeaderPhase.PARRYABLE : LeaderPhase.NORMAL;
        LeaderStateChangeCause cause = parryable
                ? LeaderStateChangeCause.WINDOW_OPENED
                : LeaderStateChangeCause.WINDOW_CLOSED;
        return transition(entity, nextPhase, cause, ignored -> {
        });
    }

    public static boolean recover(LivingEntity entity) {
        if (entity.level().isClientSide || isParryTransitionActive(entity)
                || !LeaderStateStorage.isLeader(entity)
                || LeaderStateStorage.getPhase(entity) != LeaderPhase.PARRIED) {
            return false;
        }
        return transition(entity, LeaderPhase.NORMAL, LeaderStateChangeCause.RECOVERED,
                ignored -> {
                    LeaderStateStorage.clearParriedDeadline(entity);
                    LeaderStateStorage.resetActionTicks(entity);
                });
    }

    public static void syncTo(ServerPlayer player, LivingEntity entity) {
        if (LeaderStateStorage.isLeader(entity)) {
            LeaderStateSynchronizer.syncTo(player, entity);
        }
    }

    private static boolean openParryWindow(LivingEntity entity, OptionalInt durationTicks) {
        Objects.requireNonNull(entity, "entity");
        if (isParryTransitionActive(entity)
            || !canControlExternalWindow(entity)
                || LeaderStateStorage.getPhase(entity) == LeaderPhase.PARRIED) {
            return false;
        }
        return transition(entity, LeaderPhase.PARRYABLE, LeaderStateChangeCause.WINDOW_OPENED,
                ignored -> LeaderStateStorage.setExternalWindow(entity, durationTicks));
    }

    private static boolean canControlExternalWindow(LivingEntity entity) {
        return !entity.level().isClientSide
                && LeaderStateStorage.isLeader(entity)
                && LeaderStateStorage.getProfile(entity) == LeaderProfile.EXTERNAL;
    }

    private static ParryResult commitParried(LivingEntity target, @Nullable LivingEntity actor,
                                             ResourceLocation sourceId, LeaderSnapshot oldSnapshot,
                                             int parriedTicks, int stunTicks) {
        LeaderStateStorage.clearExternalWindow(target);
        LeaderStateStorage.clearAutomaticWindowSuppression(target);
        LeaderStateStorage.resetActionTicks(target);
        LeaderStateStorage.setPhase(target, LeaderPhase.PARRIED);
        LeaderStateStorage.setParriedDeadline(target, parriedTicks);
        LeaderCombatHandler.applyParriedReaction(target, stunTicks);
        LeaderSnapshot newSnapshot = publishTransition(
                target, oldSnapshot, LeaderStateChangeCause.PARRIED);
        MinecraftForge.EVENT_BUS.post(new LeaderParriedEvent(target, actor, sourceId, newSnapshot));
        return ParryResult.SUCCESS;
    }

    private static ParryResult commitAbsorbed(LivingEntity target, @Nullable LivingEntity actor,
                                              ResourceLocation sourceId, LeaderSnapshot oldSnapshot) {
        LeaderStateStorage.clearExternalWindow(target);
        LeaderStateStorage.clearParriedDeadline(target);
        LeaderStateStorage.resetActionTicks(target);
        LeaderStateStorage.setPhase(target, LeaderPhase.NORMAL);
        if (target instanceof Naga) {
            LeaderStateStorage.suppressAutomaticWindow(target);
        }
        LeaderSnapshot newSnapshot = publishTransition(
                target, oldSnapshot, LeaderStateChangeCause.PARRY_ABSORBED);
        MinecraftForge.EVENT_BUS.post(
                new LeaderParryAbsorbedEvent(target, actor, sourceId, newSnapshot));
        return ParryResult.ABSORBED;
    }

    private static boolean isParryTransitionActive(LivingEntity entity) {
        return ACTIVE_PARRY_TRANSITIONS.contains(entity.getUUID());
    }

    private static boolean isValidParryTarget(LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved();
    }

    private static void requirePositive(int ticks, String name) {
        if (ticks <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static boolean transition(LivingEntity entity, LeaderPhase nextPhase,
                                      LeaderStateChangeCause cause,
                                      java.util.function.Consumer<LivingEntity> beforePhaseChange) {
        Optional<LeaderSnapshot> oldSnapshot = LeaderStateStorage.getSnapshot(entity);
        if (oldSnapshot.isEmpty()) {
            return false;
        }
        beforePhaseChange.accept(entity);
        LeaderStateStorage.setPhase(entity, nextPhase);
        LeaderSnapshot newSnapshot = LeaderStateStorage.getSnapshot(entity).orElseThrow();
        if (oldSnapshot.get().equals(newSnapshot)) {
            return false;
        }
        publishTransition(entity, oldSnapshot.get(), cause);
        return true;
    }

    private static LeaderSnapshot publishTransition(LivingEntity entity, LeaderSnapshot oldSnapshot,
                                                    LeaderStateChangeCause cause) {
        LeaderSnapshot newSnapshot = LeaderStateStorage.getSnapshot(entity).orElseThrow();
        LeaderStateSynchronizer.sync(entity);
        MinecraftForge.EVENT_BUS.post(new LeaderStateChangedEvent(entity, oldSnapshot, newSnapshot, cause));
        return newSnapshot;
    }
}