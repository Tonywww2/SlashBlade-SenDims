package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import com.brandon3055.draconicevolution.client.render.tile.RenderEnergyCoreStabilizer;
import com.tonywww.slashblade_sendims.client.performance.CCModelGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 稳定器大环（双面网格）经 PoseStack 包装的接收器提交，拆开包装后按同样顺序变换写入。 */
@Mixin(value = RenderEnergyCoreStabilizer.class, remap = false)
public abstract class EnergyCoreStabilizerGeometryMixin {
    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lcodechicken/lib/render/CCModel;render(Lcodechicken/lib/render/CCRenderState;[Lcodechicken/lib/render/pipeline/IVertexOperation;)V"))
    private void sdbf$emit(CCModel model, CCRenderState state, IVertexOperation[] operations) {
        CCModelGeometry.render(model, state, operations);
    }
}
