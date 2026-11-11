package com.tonywww.slashblade_sendims.compat.draconicevolution;

import net.minecraft.world.damagesource.DamageSource;

/** Implemented on {@code LivingEntity} by {@code LivingEntityDraconicShieldMixin}. */
public interface ShieldHurtTracker {
    /** The innermost open {@code hurt} frame of this entity, or null outside {@code hurt}. */
    ShieldHurtFrame sendims$shieldFrame();

    /** Applies armor durability damage now, bypassing the hold. */
    void sendims$hurtArmorNow(DamageSource source, float amount);
}
