package com.tonywww.slashblade_sendims.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Outgoing damage is multiplied once in HolySwordEffects, for every damage type. */
public final class ManaBurstEffect extends MobEffect {
    public ManaBurstEffect() { super(MobEffectCategory.BENEFICIAL, 0xFFF0C2); }
}
