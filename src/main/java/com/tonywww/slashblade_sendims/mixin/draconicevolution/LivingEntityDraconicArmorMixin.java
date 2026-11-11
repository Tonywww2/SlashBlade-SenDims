package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicShieldFinalStage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Armor durability is spent during armor absorption, before the shield's final-step decision. While a
 * Draconic shield with points is worn it is held, then spent unless the shield takes the whole hit.
 * Held at the call site, so other mods' {@code hurtArmor} hooks run once, when it is actually spent.
 * Applied only with Draconic Evolution installed (SenDimsMixinConfigPlugin).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDraconicArmorMixin {
    @Shadow protected void hurtArmor(DamageSource source, float amount) {}

    @Redirect(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void sendims$holdArmorDamage(LivingEntity self, DamageSource source, float amount) {
        if (!DraconicShieldFinalStage.holdArmorDamage(self, source, amount)) hurtArmor(source, amount);
    }
}
