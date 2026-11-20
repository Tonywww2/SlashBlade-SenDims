package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 篝火烟用 addAlwaysVisibleParticle 生成，不受粒子距离限制，存活时间也长：渲染距离内所有篝火（包括地下结构里的）
 * 都会持续堆积烟粒子，粒子 tick 的碰撞计算和渲染都要为它们付费。离镜头超过配置距离的篝火不再生成烟和食物烟。
 */
@Mixin(CampfireBlockEntity.class)
public abstract class CampfireSmokeDistanceMixin {
    @Inject(method = "particleTick", at = @At("HEAD"), cancellable = true)
    private static void sdbf$skipDistantSmoke(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire, CallbackInfo ci) {
        int distance = ClientPerformanceOptions.campfireSmokeDistance;
        if (distance <= 0) return;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (camera.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > (double) distance * distance) {
            ci.cancel();
        }
    }
}
