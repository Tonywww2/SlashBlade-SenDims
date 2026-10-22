package com.tonywww.slashblade_sendims.mixin.performance;

import com.mojang.blaze3d.platform.GlStateManager;
import com.tonywww.slashblade_sendims.client.performance.MinimapFilterState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public abstract class TextureFilterInvalidationMixin {
    @Inject(method = {"_texParameter(III)V", "_texParameter(IIF)V"}, at = @At("HEAD"))
    private static void sdbf$externalFilter(CallbackInfo callback) { MinimapFilterState.foreignWrite(); }
}
