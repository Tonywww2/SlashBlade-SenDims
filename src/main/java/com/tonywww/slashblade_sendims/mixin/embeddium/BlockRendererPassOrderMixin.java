package com.tonywww.slashblade_sendims.mixin.embeddium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Embeddium 的「渲染通道优化」逐个面挑通道：cutout 层里贴图完全不透明的面改进 solid 通道，
 * translucent 层里没有半透明像素的面改进 cutout_mipped。改过通道的面会比同一方块里先输出的面更早绘制。
 * 热力机器的流体是正面的副本，先输出、靠后画的正面盖住；机器没工作时正面贴图不透明，被挪进 solid 先画，
 * 留在 cutout 的流体反而后画，整个正面就成了流体贴图。开光影时 Embeddium 本来就不做这项优化，所以光影下正常。
 * <p>
 * 这里在同一方块的同一渲染层内保持输出顺序：一旦有面留在原通道，后面的面也都留在原通道，前面的面照常优化。
 * 目标按类名指定，没装 Embeddium 时由 mixin 插件跳过，注入不上也不报错。
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", remap = false)
public abstract class BlockRendererPassOrderMixin {
    @Unique
    private boolean sendims$keepDefaultPass;

    @Inject(method = "renderModel", at = @At("HEAD"), remap = false, require = 0)
    private void sendims$resetPassOrder(CallbackInfo ci) {
        this.sendims$keepDefaultPass = false;
    }

    @Inject(method = "chooseOptimalBuilder", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void sendims$keepPassOrder(@Coerce Object material, @Coerce Object buffers, @Coerce Object defaultBuilder,
                                       @Coerce Object quad, CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() == defaultBuilder) {
            this.sendims$keepDefaultPass = true;
        } else if (this.sendims$keepDefaultPass) {
            cir.setReturnValue(defaultBuilder);
        }
    }
}
