package com.tonywww.slashblade_sendims.mixin.integrateddynamics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tonywww.slashblade_sendims.client.performance.CableOverlayFilter;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import net.minecraft.client.renderer.MultiBufferSource;
import org.cyclops.integrateddynamics.client.render.blockentity.RenderCable;
import org.cyclops.integrateddynamics.core.blockentity.BlockEntityMultipartTicking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 线缆方块实体渲染器只为显示面板、全向连接器这类注册了覆盖层渲染器的部件画东西，
 * 但每根线缆每帧都要经 Guava 变换视图遍历一遍部件。没有这类部件的线缆在入口直接跳过。
 */
@Mixin(value = RenderCable.class, remap = false)
public abstract class RenderCableOverlayMixin {
    @Inject(method = "render(Lorg/cyclops/integrateddynamics/core/blockentity/BlockEntityMultipartTicking;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), cancellable = true)
    private void sdbf$skipCablesWithoutOverlay(BlockEntityMultipartTicking tile, float partialTicks, PoseStack poseStack,
                                               MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (ClientPerformanceOptions.integratedDynamicsCableOverlay && !CableOverlayFilter.hasOverlay(tile.getPartContainer())) {
            ci.cancel();
        }
    }
}
