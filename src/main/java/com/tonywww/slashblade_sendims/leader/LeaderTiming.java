package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SBSDValues;

final class LeaderTiming {
    private LeaderTiming() {
    }

    static boolean isManagedParryWindowOpen(int ticksBeforeAttack) {
        return ticksBeforeAttack >= -1 && ticksBeforeAttack <= SBSDValues.PARRY_TICK;
    }

    static boolean isParriedFinished(int currentTick, int endTick) {
        return currentTick > endTick;
    }

    static int remainingInclusive(int currentTick, int endTick) {
        return Math.max(0, endTick - currentTick + 1);
    }

    static boolean isDeadlineReached(long currentTime, long endTime) {
        return currentTime >= endTime;
    }

    static int remainingUntil(long currentTime, long endTime) {
        long remaining = Math.max(0L, endTime - currentTime);
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }
}