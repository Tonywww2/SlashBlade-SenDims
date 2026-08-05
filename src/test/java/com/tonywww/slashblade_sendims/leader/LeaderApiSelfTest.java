package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.api.leader.LeaderPhase;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.api.leader.LeaderSnapshot;
import com.tonywww.slashblade_sendims.api.leader.LeaderStateChangeCause;
import com.tonywww.slashblade_sendims.api.leader.ParryResult;
import com.tonywww.slashblade_sendims.network.LeaderStateSyncPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.OptionalInt;
import java.util.UUID;

public final class LeaderApiSelfTest {
    private LeaderApiSelfTest() {
    }

    public static void main(String[] args) {
        verifySnapshotValidation();
        verifyLegacyPhaseReadingDoesNotWrite();
        verifyPhaseWritesAreMutuallyExclusive();
        verifyLegacyTimingBoundaries();
        verifyDeadlineTimingBoundaries();
        verifyAppendedResultSemantics();
        verifySyncPacketRoundTrip();
    }

    private static void verifySnapshotValidation() {
        new LeaderSnapshot(LeaderProfile.EXTERNAL, LeaderPhase.NORMAL, OptionalInt.empty());
        boolean rejected = false;
        try {
            new LeaderSnapshot(LeaderProfile.MANAGED, LeaderPhase.PARRIED, OptionalInt.of(-1));
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        check(rejected, "negative remaining ticks must be rejected");
    }

    private static void verifyLegacyPhaseReadingDoesNotWrite() {
        CompoundTag data = new CompoundTag();
        int originalSize = data.getAllKeys().size();
        check(LeaderStateStorage.getServerPhase(data) == LeaderPhase.NORMAL,
                "missing legacy fields must mean NORMAL");
        check(data.getAllKeys().size() == originalSize,
                "reading a missing legacy phase must not mutate NBT");

        data.putBoolean(SBSDValues.IS_PARRIABLE_PATH, true);
        check(LeaderStateStorage.getServerPhase(data) == LeaderPhase.PARRYABLE,
                "legacy parryable flag must map to PARRYABLE");
        data.putBoolean(SBSDValues.IS_PARRIED_PATH, true);
        check(LeaderStateStorage.getServerPhase(data) == LeaderPhase.PARRIED,
                "PARRIED must take precedence over invalid dual legacy flags");
    }

    private static void verifyPhaseWritesAreMutuallyExclusive() {
        CompoundTag data = new CompoundTag();
        LeaderStateStorage.setPhase(data, LeaderPhase.PARRYABLE);
        check(data.getBoolean(SBSDValues.IS_PARRIABLE_PATH), "PARRYABLE flag must be true");
        check(!data.getBoolean(SBSDValues.IS_PARRIED_PATH), "PARRIED flag must be false");

        LeaderStateStorage.setPhase(data, LeaderPhase.PARRIED);
        check(!data.getBoolean(SBSDValues.IS_PARRIABLE_PATH), "PARRYABLE flag must be cleared");
        check(data.getBoolean(SBSDValues.IS_PARRIED_PATH), "PARRIED flag must be true");

        LeaderStateStorage.setPhase(data, LeaderPhase.NORMAL);
        check(!data.getBoolean(SBSDValues.IS_PARRIABLE_PATH), "NORMAL must clear PARRYABLE");
        check(!data.getBoolean(SBSDValues.IS_PARRIED_PATH), "NORMAL must clear PARRIED");
    }

    private static void verifyLegacyTimingBoundaries() {
        check(!LeaderTiming.isManagedParryWindowOpen(21), "window must be closed at diff 21");
        for (int diff = SBSDValues.PARRY_TICK; diff >= -1; diff--) {
            check(LeaderTiming.isManagedParryWindowOpen(diff),
                    "legacy window must remain open from diff 20 through diff -1");
        }
        check(!LeaderTiming.isManagedParryWindowOpen(-2), "window must be closed at diff -2");
        check(LeaderTiming.remainingInclusive(0, SBSDValues.PARRY_TICK + 1) == 22,
                "legacy managed window must contain 22 inclusive ticks");

        check(!LeaderTiming.isParriedFinished(SBSDValues.END_PARRIED_TICK,
                SBSDValues.END_PARRIED_TICK),
                "legacy parried phase must still be active at its end tick");
        check(LeaderTiming.isParriedFinished(SBSDValues.END_PARRIED_TICK + 1,
                SBSDValues.END_PARRIED_TICK),
                "legacy parried phase must recover after its end tick");
    }

    private static void verifyDeadlineTimingBoundaries() {
        long start = 1200L;
        long end = start + 100L;
        check(LeaderTiming.remainingUntil(start, end) == 100,
                "a 100-tick deadline must initially report 100 ticks");
        check(!LeaderTiming.isDeadlineReached(end - 1, end),
                "the deadline must remain active through its final tick");
        check(LeaderTiming.remainingUntil(end - 1, end) == 1,
                "the final active tick must report one remaining tick");
        check(LeaderTiming.isDeadlineReached(end, end),
                "the deadline must expire at the half-open end time");
        check(LeaderTiming.remainingUntil(end, end) == 0,
                "an expired deadline must report zero remaining ticks");
    }

    private static void verifyAppendedResultSemantics() {
        check(ParryResult.SUCCESS.ordinal() == 0, "SUCCESS ordinal must remain stable");
        check(ParryResult.WRONG_SIDE.ordinal() == 3, "existing result ordinals must remain stable");
        check(ParryResult.ABSORBED.ordinal() == 4, "ABSORBED must be appended");
        check(ParryResult.SUCCESS.isAccepted(), "SUCCESS must be accepted");
        check(ParryResult.ABSORBED.isAccepted(), "ABSORBED must be accepted");
        check(!ParryResult.NOT_PARRYABLE.isAccepted(), "failed results must not be accepted");
        check(LeaderStateChangeCause.PARRY_ABSORBED.ordinal() == 4,
                "PARRY_ABSORBED must be appended");
    }

    private static void verifySyncPacketRoundTrip() {
        UUID entityUuid = UUID.randomUUID();
        LeaderSnapshot snapshot = new LeaderSnapshot(
                LeaderProfile.EXTERNAL,
                LeaderPhase.PARRYABLE,
                OptionalInt.of(20)
        );
        LeaderStateSyncPacket expected = new LeaderStateSyncPacket(42, entityUuid, snapshot, 1200L);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            LeaderStateSyncPacket.encode(expected, buffer);
            LeaderStateSyncPacket actual = LeaderStateSyncPacket.decode(buffer);
            check(expected.equals(actual), "sync packet must survive encode/decode");
            check(actual.snapshot().equals(snapshot), "decoded packet must preserve the snapshot");
        } finally {
            buffer.release();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}