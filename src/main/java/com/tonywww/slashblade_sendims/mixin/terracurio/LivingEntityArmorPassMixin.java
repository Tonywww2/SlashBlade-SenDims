package com.tonywww.slashblade_sendims.mixin.terracurio;

import com.tonywww.slashblade_sendims.compat.terracurio.TerraCurioArmorPass;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Marks the armor absorption window so TerraCurio's duplicate armor pierce can be skipped. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityArmorPassMixin {
    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"))
    private void sendims$enterArmorAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        TerraCurioArmorPass.enterArmorAbsorb();
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("RETURN"))
    private void sendims$exitArmorAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        TerraCurioArmorPass.exitArmorAbsorb();
    }
}
