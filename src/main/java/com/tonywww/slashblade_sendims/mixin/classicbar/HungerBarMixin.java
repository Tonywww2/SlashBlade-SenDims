package com.tonywww.slashblade_sendims.mixin.classicbar;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tfar.classicbar.config.ClassicBarsConfig;
import tfar.classicbar.impl.overlays.vanilla.Hunger;
import tfar.classicbar.network.Message;
import tfar.classicbar.util.Color;
import tfar.classicbar.util.ModUtils;

@Mixin(Hunger.class)
public class HungerBarMixin {

    @Inject(method = "renderBar", at = @At("HEAD"), cancellable = true, remap = false)
    private void renderBarLeftToRight(ForgeGui gui, GuiGraphics matrices, Player player, int screenWidth, int screenHeight, int vOffset, CallbackInfo ci) {
        Hunger hunger = (Hunger) (Object) this;

        double hungerValue = player.getFoodData().getFoodLevel();
        double maxHunger = 20.0;
        double barWidthH = hunger.getBarWidth(player);
        double currentSat = player.getFoodData().getSaturationLevel();
        double maxSat = maxHunger;
        double barWidthS = hunger.getSatBarWidth(player);
        float exhaustion = player.getFoodData().getExhaustionLevel();

        int xStart = screenWidth / 2 + hunger.getHOffset();
        int yStart = screenHeight - vOffset;

        Color.reset();
        hunger.renderFullBarBackground(matrices, xStart, yStart);

        // 从左侧开始渲染，不使用 rightHandSide() 的偏移
        double f = xStart;
        Color hungerColor = hunger.getSecondaryBarColor(0, player);
        Color satColor = hunger.getPrimaryBarColor(0, player);

        hungerColor.color2Gl();
        hunger.renderPartialBar(matrices, f + 2.0, yStart + 2, barWidthH);

        if (currentSat > 0.0 && ClassicBarsConfig.showSaturationBar.get()) {
            satColor.color2Gl();
            f = xStart;
            hunger.renderPartialBar(matrices, f + 2.0, yStart + 2, barWidthS);
        }

        if (ClassicBarsConfig.showHeldFoodOverlay.get() && player.getMainHandItem().getItem().isEdible()) {
            ItemStack stack = player.getMainHandItem();
            double time = (double) System.currentTimeMillis() / 1000.0 * ClassicBarsConfig.transitionSpeed.get();
            double foodAlpha = Math.sin(time) / 2.0 + 0.5;
            FoodProperties food = stack.getItem().getFoodProperties(stack, player);
            if (food != null) {
                double hungerOverlay = food.getNutrition();
                double saturationMultiplier = food.getSaturationModifier();
                double potentialSat = 2.0 * hungerOverlay * saturationMultiplier;
                double hungerWidth = Math.min(maxHunger - hungerValue, hungerOverlay);
                double saturationWidth;

                if (hungerValue < maxHunger) {
                    saturationWidth = ModUtils.getWidth(hungerWidth + hungerValue, maxHunger);
                    f = xStart;
                    hungerColor.color2Gla((float) foodAlpha);
                    hunger.renderPartialBar(matrices, f + 2.0, yStart + 2, saturationWidth);
                }

                if (ClassicBarsConfig.showSaturationBar.get()) {
                    saturationWidth = Math.min(potentialSat, maxSat - currentSat);
                    saturationWidth = Math.min(saturationWidth, hungerValue + hungerWidth);
                    saturationWidth = Math.min(saturationWidth, hungerOverlay + hungerValue);

                    if (potentialSat + currentSat > hungerValue + hungerWidth) {
                        double w = potentialSat + currentSat - (hungerValue + hungerWidth);
                        saturationWidth = potentialSat - w;
                    }

                    double w = ModUtils.getWidth(saturationWidth + currentSat, maxSat);
                    f = xStart;
                    satColor.color2Gla((float) foodAlpha);
                    hunger.renderPartialBar(matrices, f + 2.0, yStart + 2, w);
                }
            }
        }

        if (ClassicBarsConfig.showExhaustionOverlay.get() && Message.presentOnServer) {
            exhaustion = Math.min(exhaustion, 4.0F);
            f = xStart;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.25F);
            ModUtils.drawTexturedModalRect(matrices, f + 2.0, yStart + 1, 1, 28, ModUtils.getWidth(exhaustion, 4.0), 9);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        ci.cancel();
    }

}
