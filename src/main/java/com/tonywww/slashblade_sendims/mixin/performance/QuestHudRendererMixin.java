package com.tonywww.slashblade_sendims.mixin.performance;

import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import com.tonywww.slashblade_sendims.client.performance.GuiQuadBatch;
import com.tonywww.slashblade_sendims.client.performance.QuestHudCaches;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.twisted_um.uiquest.client.QuestHudRenderer", remap = false)
public abstract class QuestHudRendererMixin {
    @Redirect(method = {"onRenderGuiOverlay", "renderConfirmPopup"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 0, remap = true))
    private static void sdbf$background(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (ClientPerformanceOptions.questBackground) GuiQuadBatch.fill(graphics, x1, y1, x2, y2, color);
        else graphics.fill(x1, y1, x2, y2, color);
    }

    @Redirect(method = {"onRenderGuiOverlay", "renderConfirmPopup"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V", ordinal = 1, remap = true))
    private static void sdbf$backgroundEnd(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (ClientPerformanceOptions.questBackground) graphics.flush();
        graphics.fill(x1, y1, x2, y2, color);
    }

    @Inject(method = "trimText", at = @At("HEAD"), cancellable = true)
    private static void sdbf$trimmed(Font font, String text, int width, CallbackInfoReturnable<String> callback) {
        if (!ClientPerformanceOptions.questText) { QuestHudCaches.clear(); return; }
        String cached = QuestHudCaches.get(font, text, width);
        if (cached != null) callback.setReturnValue(cached);
    }

    @Inject(method = "trimText", at = @At("RETURN"))
    private static void sdbf$remember(Font font, String text, int width, CallbackInfoReturnable<String> callback) {
        if (ClientPerformanceOptions.questText) QuestHudCaches.put(font, text, width, callback.getReturnValue());
    }
}
