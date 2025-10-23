package com.tonywww.slashblade_sendims.mixin.classicbar;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.classicbar.impl.BarOverlayImpl;
import tfar.classicbar.impl.overlays.vanilla.Air;
import tfar.classicbar.util.Color;

@Mixin(Air.class)
public abstract class AirBarMixin extends BarOverlayImpl {
    public AirBarMixin(String name) {
        super(name);
    }

    @Inject(method = "renderBar", at = @At("HEAD"), cancellable = true, remap = false)
    private void renderBarLeftToRight(ForgeGui gui, GuiGraphics graphics, Player player, int screenWidth, int screenHeight, int vOffset, CallbackInfo ci){

        int xStart = screenWidth / 2 + this.getHOffset();
        int yStart = screenHeight - vOffset;
        double barWidth = this.getBarWidth(player);
        Color.reset();
        this.renderFullBarBackground(graphics, xStart, yStart);
        double f = xStart;
        Color color = this.getPrimaryBarColor(0, player);
        color.color2Gl();
        this.renderPartialBar(graphics, f + 2.0, yStart + 2, barWidth);

        ci.cancel();
    }
}
