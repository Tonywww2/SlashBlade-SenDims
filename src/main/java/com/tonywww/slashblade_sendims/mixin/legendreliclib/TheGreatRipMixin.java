package com.tonywww.slashblade_sendims.mixin.legendreliclib;

import com.dinzeer.legendreliclib.effect.TheGreatRip;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TheGreatRip.class)
public class TheGreatRipMixin {
    @Inject(method = "onEntityHurt", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sdbf$onEntityHurt(LivingHurtEvent event, CallbackInfo ci) {
        LivingEntity victim = event.getEntity();
        victim.getActiveEffects().stream().filter((effect) -> effect.getEffect() instanceof TheGreatRip).findFirst().ifPresent((effect) -> {
            LivingEntity attacker = null;
            if (event.getSource().getEntity() instanceof LivingEntity) {
                attacker = (LivingEntity) event.getSource().getEntity();
            }

            if (attacker != null && attacker != victim) {
                int healMultiply = effect.getAmplifier() * 5 + 10;
                attacker.heal((attacker.getMaxHealth() - attacker.getHealth()) * healMultiply / 1000);
            }

        });
        ci.cancel();
    }
}
