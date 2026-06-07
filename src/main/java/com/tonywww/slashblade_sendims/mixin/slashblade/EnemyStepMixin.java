package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.ability.EnemyStep;
import mods.flammpfeil.slashblade.event.handler.InputCommandEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnemyStep.class, remap = false)
public class EnemyStepMixin {

    @Inject(method = "onInputChange(Lmods/flammpfeil/slashblade/event/handler/InputCommandEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelOnInputChange(InputCommandEvent event, CallbackInfo ci) {
        ci.cancel();
    }

}
