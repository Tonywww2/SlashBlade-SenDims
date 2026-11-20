package com.tonywww.slashblade_sendims.mixin.integrateddynamics;

import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.cyclops.cyclopscore.helper.BlockEntityHelpers;
import org.cyclops.integrateddynamics.block.BlockCable;
import org.cyclops.integrateddynamics.core.blockentity.BlockEntityMultipartTicking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 线缆亮度原本按六个方向各取一次方块实体、再查一次能力表（Pair 键哈希），
 * 每个方向的动态光照能力读的都是同一份 lightLevels，结果就是其中的最大值。
 * 这里沿用同一个取方块实体的方法（含服务端异步线程的取法），只取一次，直接读 lightLevels。
 * 不是集成动力的线缆方块实体、或方块实体已移除（能力已失效）时回到原实现。
 */
@Mixin(value = BlockCable.class, remap = false)
public abstract class BlockCableLightMixin {
    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void sdbf$lightFromLevels(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!ClientPerformanceOptions.integratedDynamicsCableLight) return;
        BlockEntity blockEntity = BlockEntityHelpers.get(level, pos, BlockEntity.class).orElse(null);
        if (blockEntity == null) {
            cir.setReturnValue(0);
            return;
        }
        if (!(blockEntity instanceof BlockEntityMultipartTicking tile) || tile.isRemoved()) return;
        int light = 0;
        for (Integer value : tile.getLightLevels().values()) {
            if (value != null && value > light) light = value;
        }
        cir.setReturnValue(light);
    }
}
