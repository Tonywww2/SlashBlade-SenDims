package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

public final class LeaderStateStorage {
    private static final String PROFILE_PATH = "sbsd.leader.profile";
    private static final String EXTERNAL_WINDOW_END_PATH = "sbsd.leader.window_end";
    private static final String EXTERNAL_WINDOW_INDEFINITE_PATH = "sbsd.leader.window_indefinite";
    private static final String PARRIED_END_PATH = "sbsd.leader.parried_end";
    private static final String AUTOMATIC_WINDOW_SUPPRESSED_PATH = "sbsd.leader.window_suppressed";
    private static final String CLIENT_SNAPSHOT_PATH = "sbsd.leader.client_snapshot";
    private static final String CLIENT_PHASE_PATH = "sbsd.leader.client_phase";
    private static final String CLIENT_REMAINING_END_PATH = "sbsd.leader.client_remaining_end";
    private static final String CLIENT_REMAINING_INDEFINITE_PATH = "sbsd.leader.client_remaining_indefinite";

    private LeaderStateStorage() {
    }

    public static boolean isLeader(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (entity.level().isClientSide) {
            return data.getBoolean(CLIENT_SNAPSHOT_PATH);
        }
        return data.contains(PROFILE_PATH)
                || data.getBoolean(SBSDValues.BOSS_LEADER)
                || data.contains(SBSDValues.APOTH_BOSS);
    }

    public static Optional<LeaderSnapshot> getSnapshot(LivingEntity entity) {
        if (!isLeader(entity)) {
            return Optional.empty();
        }

        CompoundTag data = entity.getPersistentData();
        if (entity.level().isClientSide) {
            LeaderProfile profile = readEnum(data, PROFILE_PATH, LeaderProfile.EXTERNAL);
            LeaderPhase phase = readEnum(data, CLIENT_PHASE_PATH, LeaderPhase.NORMAL);
            return Optional.of(new LeaderSnapshot(profile, phase, getClientRemainingTicks(entity, data, phase)));
        }

        LeaderProfile profile = readEnum(data, PROFILE_PATH, LeaderProfile.MANAGED);
        LeaderPhase phase = getServerPhase(data);
        return Optional.of(new LeaderSnapshot(profile, phase, getServerRemainingTicks(entity, data, profile, phase)));
    }

    static void register(LivingEntity entity, LeaderProfile profile) {
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(SBSDValues.BOSS_LEADER, true);
        data.putString(PROFILE_PATH, profile.name().toLowerCase(Locale.ROOT));
    }

    static boolean hasExplicitProfile(LivingEntity entity) {
        return entity.getPersistentData().contains(PROFILE_PATH);
    }

    static LeaderProfile getProfile(LivingEntity entity) {
        return readEnum(entity.getPersistentData(), PROFILE_PATH, LeaderProfile.MANAGED);
    }

    static LeaderPhase getPhase(LivingEntity entity) {
        return getServerPhase(entity.getPersistentData());
    }

    static void setPhase(LivingEntity entity, LeaderPhase phase) {
        setPhase(entity.getPersistentData(), phase);
    }

    static void setPhase(CompoundTag data, LeaderPhase phase) {
        data.putBoolean(SBSDValues.IS_PARRIED_PATH, phase == LeaderPhase.PARRIED);
        data.putBoolean(SBSDValues.IS_PARRIABLE_PATH, phase == LeaderPhase.PARRYABLE);
    }

    static void resetActionTicks(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.putInt(SBSDValues.LEADER_ACTION_TICK_COUNT_PATH, 0);
        data.putInt(SBSDValues.LEADER_NEXT_ACTION_TICK_COUNT_PATH, 0);
    }

    static void setExternalWindow(LivingEntity entity, OptionalInt durationTicks) {
        CompoundTag data = entity.getPersistentData();
        if (durationTicks.isPresent()) {
            data.putBoolean(EXTERNAL_WINDOW_INDEFINITE_PATH, false);
            data.putLong(EXTERNAL_WINDOW_END_PATH, entity.level().getGameTime() + durationTicks.getAsInt());
        } else {
            data.putBoolean(EXTERNAL_WINDOW_INDEFINITE_PATH, true);
            data.remove(EXTERNAL_WINDOW_END_PATH);
        }
    }

    static void clearExternalWindow(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(EXTERNAL_WINDOW_END_PATH);
        data.remove(EXTERNAL_WINDOW_INDEFINITE_PATH);
    }

    static boolean hasExternalWindowExpired(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return !data.getBoolean(EXTERNAL_WINDOW_INDEFINITE_PATH)
                && data.contains(EXTERNAL_WINDOW_END_PATH)
                && entity.level().getGameTime() >= data.getLong(EXTERNAL_WINDOW_END_PATH);
    }

    static void setParriedDeadline(LivingEntity entity, int durationTicks) {
        entity.getPersistentData().putLong(
                PARRIED_END_PATH, entity.level().getGameTime() + durationTicks);
    }

    static void clearParriedDeadline(LivingEntity entity) {
        entity.getPersistentData().remove(PARRIED_END_PATH);
    }

    static boolean hasParriedDeadline(LivingEntity entity) {
        return entity.getPersistentData().contains(PARRIED_END_PATH);
    }

    static boolean hasParriedDeadlineExpired(LivingEntity entity) {
        return LeaderTiming.isDeadlineReached(
                entity.level().getGameTime(),
                entity.getPersistentData().getLong(PARRIED_END_PATH));
    }

    static void suppressAutomaticWindow(LivingEntity entity) {
        entity.getPersistentData().putBoolean(AUTOMATIC_WINDOW_SUPPRESSED_PATH, true);
    }

    static void clearAutomaticWindowSuppression(LivingEntity entity) {
        entity.getPersistentData().remove(AUTOMATIC_WINDOW_SUPPRESSED_PATH);
    }

    static boolean isAutomaticWindowSuppressed(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(AUTOMATIC_WINDOW_SUPPRESSED_PATH);
    }

    public static void applyClientSnapshot(LivingEntity entity, LeaderSnapshot snapshot, long serverGameTime) {
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(CLIENT_SNAPSHOT_PATH, true);
        data.putString(PROFILE_PATH, snapshot.profile().name().toLowerCase(Locale.ROOT));
        data.putString(CLIENT_PHASE_PATH, snapshot.phase().name().toLowerCase(Locale.ROOT));
        if (snapshot.remainingTicks().isPresent()) {
            data.putBoolean(CLIENT_REMAINING_INDEFINITE_PATH, false);
            data.putLong(CLIENT_REMAINING_END_PATH,
                    serverGameTime + snapshot.remainingTicks().getAsInt());
        } else {
            data.putBoolean(CLIENT_REMAINING_INDEFINITE_PATH, true);
            data.remove(CLIENT_REMAINING_END_PATH);
        }
    }

    static LeaderPhase getServerPhase(CompoundTag data) {
        if (data.getBoolean(SBSDValues.IS_PARRIED_PATH)) {
            return LeaderPhase.PARRIED;
        }
        if (data.getBoolean(SBSDValues.IS_PARRIABLE_PATH)) {
            return LeaderPhase.PARRYABLE;
        }
        return LeaderPhase.NORMAL;
    }

    private static OptionalInt getServerRemainingTicks(LivingEntity entity, CompoundTag data,
                                                       LeaderProfile profile, LeaderPhase phase) {
        if (phase == LeaderPhase.PARRYABLE && profile == LeaderProfile.EXTERNAL) {
            if (data.getBoolean(EXTERNAL_WINDOW_INDEFINITE_PATH)) {
                return OptionalInt.empty();
            }
            if (data.contains(EXTERNAL_WINDOW_END_PATH)) {
                return OptionalInt.of((int) Math.max(0L,
                        data.getLong(EXTERNAL_WINDOW_END_PATH) - entity.level().getGameTime()));
            }
        }
        if (phase == LeaderPhase.PARRYABLE) {
            int targetTick = data.getInt(SBSDValues.LEADER_NEXT_ACTION_TICK_COUNT_PATH);
            int currentTick = data.getInt(SBSDValues.LEADER_ACTION_TICK_COUNT_PATH);
            if (targetTick > 0) {
                return OptionalInt.of(LeaderTiming.remainingInclusive(currentTick, targetTick + 1));
            }
        }
        if (phase == LeaderPhase.PARRIED) {
            if (data.contains(PARRIED_END_PATH)) {
                return OptionalInt.of(LeaderTiming.remainingUntil(
                        entity.level().getGameTime(), data.getLong(PARRIED_END_PATH)));
            }
            int endTick = data.getInt(SBSDValues.LEADER_NEXT_ACTION_TICK_COUNT_PATH);
            int currentTick = data.getInt(SBSDValues.LEADER_ACTION_TICK_COUNT_PATH);
            int effectiveEndTick = endTick > 0 ? endTick : SBSDValues.END_PARRIED_TICK;
            return OptionalInt.of(LeaderTiming.remainingInclusive(currentTick, effectiveEndTick));
        }
        return OptionalInt.empty();
    }

    private static OptionalInt getClientRemainingTicks(LivingEntity entity, CompoundTag data, LeaderPhase phase) {
        if (phase == LeaderPhase.NORMAL || data.getBoolean(CLIENT_REMAINING_INDEFINITE_PATH)) {
            return OptionalInt.empty();
        }
        if (!data.contains(CLIENT_REMAINING_END_PATH)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((int) Math.max(0L,
                data.getLong(CLIENT_REMAINING_END_PATH) - entity.level().getGameTime()));
    }

    private static <T extends Enum<T>> T readEnum(CompoundTag data, String path, T fallback) {
        if (!data.contains(path)) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), data.getString(path).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}