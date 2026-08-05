package com.tonywww.slashblade_sendims.api.leader;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * An immutable Leader state view available on both logical sides.
 * An empty remaining duration means that the current phase has no known deadline.
 */
public record LeaderSnapshot(LeaderProfile profile, LeaderPhase phase, OptionalInt remainingTicks) {
    public LeaderSnapshot {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(remainingTicks, "remainingTicks");
        if (remainingTicks.isPresent() && remainingTicks.getAsInt() < 0) {
            throw new IllegalArgumentException("remainingTicks must not be negative");
        }
    }
}