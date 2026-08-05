package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.api.leader.LeaderStateChangeCause;
import com.tonywww.slashblade_sendims.api.leader.ParryResult;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderParriedEvent;
import com.tonywww.slashblade_sendims.api.leader.event.LeaderStateChangedEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

public final class LeaderManager {
    private static final Map<EntityType<?>, LeaderProfile> TYPE_PROFILES = new ConcurrentHashMap<>();

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
        if (!canControlExternalWindow(entity)
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
        if (LeaderStateStorage.getPhase(target) != LeaderPhase.PARRYABLE) {
            return ParryResult.NOT_PARRYABLE;
        }

        LeaderSnapshot oldSnapshot = LeaderStateStorage.getSnapshot(target).orElseThrow();
        LeaderStateStorage.clearExternalWindow(target);
        LeaderStateStorage.setPhase(target, LeaderPhase.PARRIED);
        LeaderStateStorage.resetActionTicks(target);
        LeaderCombatHandler.applyParriedReaction(target);
        LeaderSnapshot newSnapshot = publishTransition(
                target, oldSnapshot, LeaderStateChangeCause.PARRIED);
        MinecraftForge.EVENT_BUS.post(new LeaderParriedEvent(target, actor, sourceId, newSnapshot));
        return ParryResult.SUCCESS;
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
        if (entity.level().isClientSide || !LeaderStateStorage.isLeader(entity)
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
        if (entity.level().isClientSide || !LeaderStateStorage.isLeader(entity)
                || LeaderStateStorage.getPhase(entity) != LeaderPhase.PARRIED) {
            return false;
        }
        return transition(entity, LeaderPhase.NORMAL, LeaderStateChangeCause.RECOVERED,
                ignored -> LeaderStateStorage.resetActionTicks(entity));
    }

    public static void syncTo(ServerPlayer player, LivingEntity entity) {
        if (LeaderStateStorage.isLeader(entity)) {
            LeaderStateSynchronizer.syncTo(player, entity);
        }
    }

    private static boolean openParryWindow(LivingEntity entity, OptionalInt durationTicks) {
        Objects.requireNonNull(entity, "entity");
        if (!canControlExternalWindow(entity)
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