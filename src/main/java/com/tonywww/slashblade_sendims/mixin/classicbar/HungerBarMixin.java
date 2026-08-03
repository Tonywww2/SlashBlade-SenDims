package com.tonywww.slashblade_sendims.mixin.classicbar;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tfar.classicbar.api.BarSide;
import tfar.classicbar.impl.overlays.templates.FoodLikeBarOverlay;
import tfar.classicbar.impl.overlays.vanilla.Food;

@Mixin(FoodLikeBarOverlay.class)
public class HungerBarMixin {
    @Redirect(
            method = {
                    "drawThirst",
                    "drawHydration",
                    "drawExhaustion",
                    "drawOverlayPrediction"
            },
            at = @At(value = "INVOKE", target = "Ltfar/classicbar/impl/overlays/templates/FoodLikeBarOverlay;getSide()Ltfar/classicbar/api/BarSide;"),
            remap = false
    )
    private BarSide renderFoodLeftToRight(FoodLikeBarOverlay overlay) {
        return overlay instanceof Food ? BarSide.LEFT : overlay.getSide();
    }
}
