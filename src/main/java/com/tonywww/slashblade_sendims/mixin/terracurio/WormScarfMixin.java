package com.tonywww.slashblade_sendims.mixin.terracurio;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Worm Scarf: damage taken ×0.83 (17% reduction) → ×0.99 (1% reduction). */
@Pseudo
@Mixin(targets = "org.confluence.terra_curio.item.curio.expert.WormScarf", remap = false)
public abstract class WormScarfMixin {
    private static final float DAMAGE_TAKEN_MULTIPLIER = 0.99F;

    @ModifyConstant(method = "apply(Lnet/minecraft/world/entity/LivingEntity;F)F",
            constant = @Constant(floatValue = 0.83F), remap = false)
    private static float sendims$damageTakenMultiplier(float original) {
        return DAMAGE_TAKEN_MULTIPLIER;
    }
}
