package com.tonywww.slashblade_sendims.mixin.performance;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tonywww.slashblade_sendims.client.performance.MinimapFilterState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbchunks.client.mapicon.EntityMapIcon", remap = false)
public abstract class FtbEntityMapIconMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private void sdbf$icon(CallbackInfo callback) { MinimapFilterState.beginIcon(); }

    @Redirect(method = "draw", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;bindTextureForSetup(I)V", remap = false))
    private void sdbf$bind(int texture) {
        MinimapFilterState.bind(texture);
        RenderSystem.bindTextureForSetup(texture);
    }

    @Redirect(method = "draw", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;texParameter(III)V", remap = false), require = 2, allow = 2)
    private void sdbf$filter(int target, int parameter, int value) { MinimapFilterState.parameter(target, parameter, value); }
}
