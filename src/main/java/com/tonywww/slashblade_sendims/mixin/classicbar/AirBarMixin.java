package com.tonywww.slashblade_sendims.mixin.classicbar;

import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tfar.classicbar.api.BarSide;
import tfar.classicbar.impl.overlays.templates.BarOverlayImpl;
import tfar.classicbar.impl.overlays.vanilla.Air;

@Mixin(BarOverlayImpl.class)
public abstract class AirBarMixin {
    @Redirect(
            method = "renderSimpleBar(Ltfar/classicbar/api/Color;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIZ)V",
            at = @At(value = "INVOKE", target = "Ltfar/classicbar/impl/overlays/templates/BarOverlayImpl;getXStartBar(II)I"),
            remap = false
    )
    private int renderAirLeftToRight(BarOverlayImpl overlay, int screenWidth, int barWidth) {
        int xStart = screenWidth / 2 + overlay.getHOffset();
        if (!(overlay instanceof Air) && overlay.getSide() == BarSide.RIGHT) {
            xStart += 77 - barWidth;
        }
        return xStart;
    }
}
