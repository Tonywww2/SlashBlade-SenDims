package com.tonywww.slashblade_sendims.se;

public final class HolySwordRulesSelfTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        check(!HolySwordRules.ready(19, false) && HolySwordRules.ready(20, false), "20-hit threshold");
        check(!HolySwordRules.ready(13, true) && HolySwordRules.ready(14, true), "14-hit threshold");
        check(!HolySwordRules.ready(14, false), "Losing A rank must restore the 20-hit requirement");
        check(HolySwordRules.addHit(20) == 20 && HolySwordRules.addHit(-1) == 1, "Charge bounds");
        check(HolySwordRules.amplifier(false) == 1 && HolySwordRules.duration(false) == 100, "II / 5 seconds");
        check(HolySwordRules.amplifier(true) == 2 && HolySwordRules.duration(true) == 140, "III / 7 seconds");
        check(Math.abs(HolySwordRules.damageMultiplier(1) - 1.10) < 1e-9, "Level II gives exactly +10%");
        check(Math.abs(HolySwordRules.damageMultiplier(2) - 1.15) < 1e-9, "Level III gives exactly +15%");
        check(!HolySwordRules.outsideWindRange(36.0) && HolySwordRules.outsideWindRange(36.00001), "Strictly beyond 6 blocks");
        check(HolySwordRules.CALIBURN_OPENING + HolySwordRules.CALIBURN_FINISH == 13, "Caliburn 13x physical");
        float physical = 0, magic = 0;
        for (int i = 0; i < 3; i++) {
            physical += HolySwordRules.excaliburPhysical(i); magic += HolySwordRules.excaliburMagic(i);
        }
        check(physical == 7 && magic == 7 && physical + magic == 14, "Excalibur 7x physical + 7x magic");
        System.out.println("Holy sword rules: all 11 checks passed.");
    }
}
