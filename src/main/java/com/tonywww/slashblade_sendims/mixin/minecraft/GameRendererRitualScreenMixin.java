package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.client.ritual.RitualScreenEffect;
import com.tonywww.slashblade_sendims.client.zenith.ZenithRiftScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 屏幕效果的挂点。
 * <ul>
 *   <li>仪式：renderLevel 返回之后、界面绘制之前。不用 Forge 的 AFTER_LEVEL：它在 LevelRenderer.renderLevel 内部，
 *       光影包的最终合成要到其后才完成。</li>
 *   <li>真·次元斩：HUD（含小地图等 HUD 叠加层）画完之后、打开的界面之前，让 HUD 也一起定格、碎裂，
 *       暂停菜单、背包、聊天仍画在上面可用。按 F1 隐藏界面时 HUD 不画，改在 renderLevel 之后。</li>
 * </ul>
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererRitualScreenMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void sendims$ritualScreenEffect(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        RitualScreenEffect.render(partialTick);
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui && mc.screen == null) ZenithRiftScreen.render(partialTick);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;F)V"))
    private void sendims$zenithRiftScreen(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (!ZenithRiftScreen.active()) return;
        // HUD 有一部分还在批处理缓冲里，先画出来，定格和碎裂才包含完整的 HUD。
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        ZenithRiftScreen.render(partialTick);
    }
}
