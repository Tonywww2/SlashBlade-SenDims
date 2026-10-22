package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Vector3;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileGrinder;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import com.tonywww.slashblade_sendims.client.performance.GrinderSwordGeometry;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderTileGrinder.class, remap = false)
public abstract class GrinderGeometryMixin {
    @Shadow @Final private CCModel swordModel;
    @Shadow @Final private static double[] ROTATION_MAP;
    @Unique private final GrinderSwordGeometry sdbf$geometry = new GrinderSwordGeometry();

    @Inject(method = "renderSword", at = @At("HEAD"), cancellable = true)
    private void sdbf$idle(CCRenderState state, Direction facing, double side, Vector3 tile, Vector3 target,
                           double animation, float partialTick, CallbackInfo callback) {
        if (!ClientPerformanceOptions.grinderGeometry) { sdbf$geometry.clear(); return; }
        int direction = facing.get3DDataValue() - 2;
        // Both endpoints have yoyo == 0 in the upstream animation.
        if ((animation == 0 || animation == 1) && target == null && direction >= 0 && direction < ROTATION_MAP.length
                && sdbf$geometry.idle(swordModel, state, direction, side, ROTATION_MAP[direction] * .017453292519943)) {
            callback.cancel();
        }
    }

    @Redirect(method = "renderSword", at = @At(value = "INVOKE",
            target = "Lcodechicken/lib/render/CCModel;render(Lcodechicken/lib/render/CCRenderState;[Lcodechicken/lib/render/pipeline/IVertexOperation;)V"))
    private void sdbf$submit(CCModel model, CCRenderState state, IVertexOperation[] operations) {
        if (!ClientPerformanceOptions.grinderGeometry || !sdbf$geometry.animated(model, state, operations)) model.render(state, operations);
    }
}
