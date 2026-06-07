package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.ability.KickJump;
import mods.flammpfeil.slashblade.event.handler.InputCommandEvent;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KickJump.class, remap = false)
public class KickJumpMixin {

    @Inject(method = "onInputChange(Lmods/flammpfeil/slashblade/event/handler/InputCommandEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelOnInputChange(InputCommandEvent event, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onTick(Lnet/minecraftforge/event/TickEvent$PlayerTickEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelOnTick(TickEvent.PlayerTickEvent event, CallbackInfo ci) {
        ci.cancel();
    }

}
