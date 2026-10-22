package com.tonywww.slashblade_sendims.se;

/** Shared, dependency-free rules for the approved Caliburn/Excalibur design. */
public final class HolySwordRules {
    public static final int NORMAL_HITS = 20;
    public static final int ROYAL_HITS = 14;
    public static final int NORMAL_DURATION = 100;
    public static final int ROYAL_DURATION = 140;
    public static final double DAMAGE_PER_LEVEL = 0.05D;
    public static final double WIND_DISTANCE_SQUARED = 36.0D;
    public static final float WIND_MULTIPLIER = 0.9F;
    public static final float CALIBURN_OPENING = 3.0F;
    public static final float CALIBURN_FINISH = 10.0F;

    private HolySwordRules() {}

    public static int requiredHits(boolean royal) { return royal ? ROYAL_HITS : NORMAL_HITS; }
    // Minecraft amplifiers are zero-based: amplifier 1 is displayed as level II.
    public static int amplifier(boolean royal) { return royal ? 2 : 1; }
    public static int duration(boolean royal) { return royal ? ROYAL_DURATION : NORMAL_DURATION; }
    public static int addHit(int hits) { return Math.min(NORMAL_HITS, Math.max(0, hits) + 1); }
    public static boolean ready(int hits, boolean royal) { return hits >= requiredHits(royal); }
    public static double damageMultiplier(int amplifier) { return 1.0D + DAMAGE_PER_LEVEL * (amplifier + 1); }
    public static boolean outsideWindRange(double distanceSquared) { return distanceSquared > WIND_DISTANCE_SQUARED; }
    public static float excaliburPhysical(int phase) {
        if (phase < 0 || phase > 2) throw new IllegalArgumentException("Invalid light pulse: " + phase);
        return phase == 2 ? 3.0F : 2.0F;
    }
    public static float excaliburMagic(int phase) { return excaliburPhysical(phase); }
}
