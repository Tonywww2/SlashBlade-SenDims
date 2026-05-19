package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.event.BladeExEvent;
import mods.flammpfeil.slashblade.event.SlashBladeRegistryEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BladeExEvent.class, remap = false)
public class BladeExEventMixin {

    @Inject(method = "NihilBxAddSe", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onNihilBxAddSe(SlashBladeRegistryEvent.Post event, CallbackInfo ci) {
        ci.cancel();
    }
}

