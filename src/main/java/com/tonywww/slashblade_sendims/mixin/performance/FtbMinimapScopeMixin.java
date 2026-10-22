package com.tonywww.slashblade_sendims.mixin.performance;

import com.tonywww.slashblade_sendims.client.performance.MinimapFilterState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.FTBChunksClient", remap = false)
public abstract class FtbMinimapScopeMixin {
    @Inject(method = "renderHud", at = @At("HEAD"))
    private void sdbf$begin(CallbackInfo callback) { MinimapFilterState.begin(); }

    @Inject(method = "renderHud", at = @At("RETURN"))
    private void sdbf$end(CallbackInfo callback) { MinimapFilterState.end(); }
}
