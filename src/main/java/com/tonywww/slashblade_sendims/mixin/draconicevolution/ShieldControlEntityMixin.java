package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.draconicevolution.api.modules.entities.ShieldControlEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draconic shields no longer absorb whole hits in {@code LivingAttackEvent} against the raw damage.
 * The enable check, unblockable types and environmental blocking (fire, lava and similar at a flat
 * cost) that come before still run; all other absorption happens in
 * {@link com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicShieldFinalStage}.
 */
@Mixin(value = ShieldControlEntity.class, remap = false)
public abstract class ShieldControlEntityMixin {
    @Inject(method = "tryBlockDamage(Lnet/minecraftforge/event/entity/living/LivingAttackEvent;)V",
            at = @At(value = "INVOKE", target = "Lcom/brandon3055/draconicevolution/api/modules/entities/ShieldControlEntity;applyDamageModifiers(Lnet/minecraft/world/damagesource/DamageSource;F)F"),
            cancellable = true)
    private void sendims$deferShieldAbsorption(LivingAttackEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
