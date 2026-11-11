package com.tonywww.slashblade_sendims.mixin.ftbchunks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FTB Chunks 大地图不再打开（地图由 Xaero 提供）。 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen", remap = false)
public abstract class LargeMapScreenMixin {
    @Inject(method = "openMap", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sdbf$noLargeMap(CallbackInfo ci) {
        ci.cancel();
    }
}
