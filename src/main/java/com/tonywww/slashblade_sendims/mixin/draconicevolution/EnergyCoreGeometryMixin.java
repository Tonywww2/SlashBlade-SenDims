package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyCore;
import com.tonywww.slashblade_sendims.client.performance.CCModelGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 内核、外壳和（未开光影时）稳定器球体的网格提交；光影下的稳定器由 DE Render Patcher 自己绘制，不经过这里。 */
@Mixin(value = RenderTileEnergyCore.class, remap = false)
public abstract class EnergyCoreGeometryMixin {
    @Redirect(method = {"renderInnerCore", "renderFancyOuterCore", "renderLegacyOuterCore", "renderStabilizers"},
            at = @At(value = "INVOKE",
                    target = "Lcodechicken/lib/render/CCModel;render(Lcodechicken/lib/render/CCRenderState;[Lcodechicken/lib/render/pipeline/IVertexOperation;)V"))
    private void sdbf$emit(CCModel model, CCRenderState state, IVertexOperation[] operations) {
        CCModelGeometry.render(model, state, operations);
    }
}
