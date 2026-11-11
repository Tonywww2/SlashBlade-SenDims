package com.tonywww.slashblade_sendims.compat.draconicevolution;

import net.minecraft.world.damagesource.DamageSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Bookkeeping for one {@code actuallyHurt} call made by {@code LivingEntity.hurt}. Frames nest when an
 * entity is hurt again from inside its own damage pipeline.
 */
public final class ShieldHurtFrame {
    final ShieldHurtFrame outer;
    final int priorInvulnerableTime;
    final float priorLastHurt;
    /** Armor durability damage held back until the shield decides; null when nothing is held. */
    List<PendingArmor> pendingArmor;
    boolean blocked;

    ShieldHurtFrame(ShieldHurtFrame outer, int priorInvulnerableTime, float priorLastHurt) {
        this.outer = outer;
        this.priorInvulnerableTime = priorInvulnerableTime;
        this.priorLastHurt = priorLastHurt;
    }

    public ShieldHurtFrame outer() { return outer; }
    public int priorInvulnerableTime() { return priorInvulnerableTime; }
    public float priorLastHurt() { return priorLastHurt; }

    void hold(DamageSource source, float amount) {
        if (pendingArmor == null) pendingArmor = new ArrayList<>(2);
        pendingArmor.add(new PendingArmor(source, amount));
    }

    record PendingArmor(DamageSource source, float amount) {}
}
