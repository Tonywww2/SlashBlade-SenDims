package com.tonywww.slashblade_sendims.mixin.sketchrender;

import com.tonywww.slashblade_sendims.client.performance.SketchIndexCapacity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * SketchRender 0.1.0a：绘制区块前按局部变量 maxElement 扩容共享索引缓冲，它来自显卡读回，可能是旧值。
 * 这里把它换成读回值与 CPU 记录值（{@link SketchIndexCapacity}）中的较大者，之后由 SketchRender 自己的逻辑扩容。
 * 目标按类名指定，没装 SketchRender 时由 mixin 插件跳过，注入不上也不报错。
 */
@Pseudo
@Mixin(targets = "rogo.sketch.compat.sodium.IndirectDrawChunkRenderer", remap = false)
public abstract class SketchIndirectChunkRendererMixin {
    @ModifyVariable(method = "render", at = @At("STORE"), name = "maxElement", remap = false, require = 0)
    private int sendims$cpuMaxElement(int maxElement) {
        return Math.max(maxElement, SketchIndexCapacity.maxElements());
    }
}
