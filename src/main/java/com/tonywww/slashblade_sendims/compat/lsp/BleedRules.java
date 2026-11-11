package com.tonywww.slashblade_sendims.compat.lsp;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Numbers for the lethal_slash_project bleed gauge (攻击累计出血量表), kept free of lsp classes.
 *
 * <ul>
 *     <li>Per hit: {@code 30 + 200% attack}.</li>
 *     <li>Burst when the gauge is full: {@code lost HP × 15% + attacker attack}, dealt in 4 hits.</li>
 *     <li>Decay: after 5 s without being hurt, once per second remove 1% of the gauge cap, doubling
 *     each time up to 16% (1, 2, 4, 8, 16, 16...). Any hurt restarts the cycle.</li>
 * </ul>
 */
public final class BleedRules {
    public static final int PER_HIT_FLAT = 30;
    public static final double PER_HIT_ATTACK_RATIO = 2.0D;
    public static final float BURST_LOST_HEALTH_RATIO = 0.15F;
    public static final int BURST_HITS = 4;

    public static final int DECAY_DELAY_TICKS = 100;
    public static final int DECAY_INTERVAL_TICKS = 20;
    public static final int DECAY_START_PERCENT = 1;
    public static final int DECAY_MAX_PERCENT = 16;

    private BleedRules() {
    }

    /** Gauge cap: the target's max health, at least 1. */
    public static int limit(LivingEntity target) {
        return Math.max(1, (int) Math.floor(target.getMaxHealth()));
    }

    public static int perHit(double attack) {
        return PER_HIT_FLAT + (int) Math.floor(Math.max(0.0D, attack) * PER_HIT_ATTACK_RATIO);
    }

    public static float burstDamage(float maxHealth, float health, double attack) {
        float lost = Math.max(0.0F, maxHealth - health);
        return lost * BURST_LOST_HEALTH_RATIO + (float) Math.max(0.0D, attack);
    }

    /** Attack damage of an entity, 0 when it has none (or is not a living entity). */
    public static double attackOf(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return 0.0D;
        AttributeInstance attack = living.getAttribute(Attributes.ATTACK_DAMAGE);
        return attack == null ? 0.0D : attack.getValue();
    }

    /**
     * Percent of the cap to remove on this tick, or 0 when this tick is not a decay tick.
     *
     * @param unhurtTicks game ticks since the target last took damage
     */
    public static int decayPercent(long unhurtTicks) {
        if (unhurtTicks < DECAY_DELAY_TICKS) return 0;
        long since = unhurtTicks - DECAY_DELAY_TICKS;
        if (since % DECAY_INTERVAL_TICKS != 0) return 0;
        long step = since / DECAY_INTERVAL_TICKS;
        long percent = DECAY_START_PERCENT;
        for (long i = 0; i < step && percent < DECAY_MAX_PERCENT; i++) {
            percent <<= 1;
        }
        return (int) Math.min(DECAY_MAX_PERCENT, percent);
    }

    public static int decayAmount(int limit, long unhurtTicks) {
        int percent = decayPercent(unhurtTicks);
        if (percent <= 0) return 0;
        return Math.max(1, (int) Math.ceil(limit * percent / 100.0D));
    }
}
