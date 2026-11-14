package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.client.ritual.RitualScreenEffect;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 仪式屏幕效果的挂点：renderLevel 返回之后、界面绘制之前。
 * 不用 Forge 的 AFTER_LEVEL：它在 LevelRenderer.renderLevel 内部，光影包的最终合成要到其后才完成。
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererRitualScreenMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void sendims$ritualScreenEffect(float partialTick, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        RitualScreenEffect.render(partialTick);
    }
}
