package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 刷怪笼的客户端 tick 只负责笼内生物旋转和火焰粒子，但每个刷怪笼每 tick 都要先判断"附近有没有玩家"，
 * Botania 生命灌注器和 PneumaticCraft 刷怪笼搅动器各在这个判断里再查一次方块/半方块。
 * 原版只在 64 格内渲染刷怪笼、32 格内生成它的粒子，所以离镜头超过配置距离的刷怪笼跳过客户端 tick，画面没有差别。
 * 神化的刷怪笼沿用同一个 ticker，一并生效。
 */
@Mixin(SpawnerBlockEntity.class)
public abstract class SpawnerClientTickDistanceMixin {
    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true)
    private static void sdbf$skipDistantSpawners(Level level, BlockPos pos, BlockState state, SpawnerBlockEntity spawner, CallbackInfo ci) {
        int distance = ClientPerformanceOptions.spawnerClientTickDistance;
        if (distance <= 0) return;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (camera.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > (double) distance * distance) {
            ci.cancel();
        }
    }
}
