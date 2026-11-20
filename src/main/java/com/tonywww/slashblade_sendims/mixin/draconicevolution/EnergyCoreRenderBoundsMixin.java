package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.brandonscore.lib.datamanager.ManagedPos;
import com.brandon3055.draconicevolution.blocks.tileentity.TileEnergyCore;
import com.brandon3055.draconicevolution.client.render.tile.RenderTileEnergyCore;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 能量核心的渲染范围原本是无限大，又是全局方块实体，看不见时也每帧完整绘制。
 * 改为按实际绘制内容给出有限范围：核心外壳半径 = SCALES[等级] × 0.7 × 模型半径 1.0，
 * 稳定器球、光束都在核心与各稳定器之间，横向不超过 2.8 格，统一留 4 格余量。
 * 打开建造指引时会画整座多方块结构，保持原来的无限范围。
 */
@Mixin(value = TileEnergyCore.class, remap = false)
public abstract class EnergyCoreRenderBoundsMixin {
    @Inject(method = "getRenderBoundingBox", at = @At("HEAD"), cancellable = true)
    private void sdbf$finiteBounds(CallbackInfoReturnable<AABB> cir) {
        if (!ClientPerformanceOptions.draconicEnergyCoreBounds) return;
        TileEnergyCore core = (TileEnergyCore) (Object) this;
        if (core.buildGuide.get()) return;
        double[] scales = RenderTileEnergyCore.SCALES;
        double reach = scales[Mth.clamp(core.tier.get(), 1, scales.length) - 1] * 0.7 * 1.1 + 1.0;
        if (core.stabilizersValid.get()) {
            for (ManagedPos managed : core.stabilizerPositions) {
                BlockPos offset = managed == null ? null : managed.get();
                if (offset == null) continue;
                int far = Math.max(Math.abs(offset.getX()), Math.max(Math.abs(offset.getY()), Math.abs(offset.getZ())));
                reach = Math.max(reach, far + 4.0);
            }
        }
        BlockPos pos = core.getBlockPos();
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        cir.setReturnValue(new AABB(x - reach, y - reach, z - reach, x + reach, y + reach, z + reach));
    }
}
