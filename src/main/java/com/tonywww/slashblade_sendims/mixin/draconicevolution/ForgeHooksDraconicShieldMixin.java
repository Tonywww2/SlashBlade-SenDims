package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicShieldFinalStage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code LivingEntity.actuallyHurt} and {@code Player.actuallyHurt} use this return value directly
 * for {@code setHealth}: armor, protection, absorption hearts and every {@code LivingDamageEvent}
 * listener have already run. The Draconic shield absorbs here, as the last step.
 * Applied only with Draconic Evolution installed (SenDimsMixinConfigPlugin).
 */
@Mixin(value = ForgeHooks.class, remap = false)
public abstract class ForgeHooksDraconicShieldMixin {
    @Inject(method = "onLivingDamage", at = @At("RETURN"), cancellable = true)
    private static void sendims$draconicShieldLast(LivingEntity entity, DamageSource source, float amount,
                                                   CallbackInfoReturnable<Float> cir) {
        float damage = cir.getReturnValueF();
        float left = DraconicShieldFinalStage.finalDamage(entity, source, damage);
        if (left != damage) cir.setReturnValue(left);
    }
}
