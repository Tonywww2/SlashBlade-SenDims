package com.tonywww.slashblade_sendims.mixin.terracurio;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Frozen Turtle Shell: damage taken at or below 50% health ×0.75 (25% reduction) → ×0.95 (5% reduction).
 * The original trigger condition is kept, including its requirement that the Frozen Shield is also worn.
 */
@Pseudo
@Mixin(targets = "org.confluence.terra_curio.item.curio.combat.FrozenTurtleShell", remap = false)
public abstract class FrozenTurtleShellMixin {
    private static final float DAMAGE_TAKEN_MULTIPLIER = 0.95F;

    @ModifyConstant(method = "apply(Lnet/minecraft/world/entity/LivingEntity;F)F",
            constant = @Constant(floatValue = 0.75F), remap = false)
    private static float sendims$damageTakenMultiplier(float original) {
        return DAMAGE_TAKEN_MULTIPLIER;
    }
}
