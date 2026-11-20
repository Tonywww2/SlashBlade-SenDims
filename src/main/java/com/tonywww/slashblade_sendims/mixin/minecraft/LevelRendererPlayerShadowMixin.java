package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import com.tonywww.slashblade_sendims.client.performance.IrisShadowPass;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 光影阴影通道（Oculus 的 ShadowRenderer）也经由 renderEntity 绘制玩家；装了 Yes Steve Model 时，
 * 玩家模型在阴影通道里再算一遍动画和网格。关闭开关后，只在阴影通道里跳过玩家，主画面不变。
 * 没装 YSM、没开光影（不在阴影通道）或开关打开时完全不介入。
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererPlayerShadowMixin {
    @Unique
    private static int sdbf$ysmState;

    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void sdbf$skipYsmPlayerShadow(Entity entity, double cameraX, double cameraY, double cameraZ, float partialTick,
                                          PoseStack poseStack, MultiBufferSource buffers, CallbackInfo ci) {
        if (!(entity instanceof Player) || ClientPerformanceOptions.ysmPlayerShadows) return;
        if (sdbf$ysmState == 0) sdbf$ysmState = ModList.get().isLoaded("yes_steve_model") ? 1 : 2;
        if (sdbf$ysmState == 1 && IrisShadowPass.isActive()) ci.cancel();
    }
}
