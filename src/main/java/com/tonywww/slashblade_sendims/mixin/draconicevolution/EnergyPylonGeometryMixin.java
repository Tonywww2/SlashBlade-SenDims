package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyPylon;
import com.tonywww.slashblade_sendims.client.performance.CCModelGeometry;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import net.minecraft.client.renderer.RenderType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 能量塔每帧都用完全相同的参数重建一次外壳渲染类型并覆盖静态字段，
 * 每帧都是新的 RenderType 键，批缓冲按新键另起一份。保留类加载时建好的那一个，参数与外观不变。
 */
@Mixin(value = RenderTileEnergyPylon.class, remap = false)
public abstract class EnergyPylonGeometryMixin {
    @Shadow
    private static RenderType shellType;

    @Redirect(method = "render", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC,
            target = "Lcom/brandon3055/draconicevolution/client/render/tile/RenderTileEnergyPylon;shellType:Lnet/minecraft/client/renderer/RenderType;"))
    private void sdbf$keepShellType(RenderType rebuilt) {
        if (!ClientPerformanceOptions.draconicModelGeometry || shellType == null) shellType = rebuilt;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lcodechicken/lib/render/CCModel;render(Lcodechicken/lib/render/CCRenderState;[Lcodechicken/lib/render/pipeline/IVertexOperation;)V"))
    private void sdbf$emit(CCModel model, CCRenderState state, IVertexOperation[] operations) {
        CCModelGeometry.render(model, state, operations);
    }
}
