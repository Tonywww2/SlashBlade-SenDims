package com.tonywww.slashblade_sendims.mixin.ftbchunks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 圈地改在 Xaero 世界地图里进行，FTB Chunks 的圈地界面不再打开。 */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.gui.ChunkScreen", remap = false)
public abstract class ChunkScreenMixin {
    @Inject(method = "openChunkScreen", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sdbf$noClaimScreen(CallbackInfo ci) {
        ci.cancel();
    }
}
