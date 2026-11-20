package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.brandonscore.client.BCClientEventHandler;
import com.brandon3055.brandonscore.client.render.BlockEntityRendererTransparent;
import com.tonywww.slashblade_sendims.client.performance.ClientPerformanceOptions;
import com.tonywww.slashblade_sendims.mixin.minecraft.BlockEntityRenderDispatcherAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Brandon's Core 的透明通道每帧遍历所有可见区块里的全部方块实体：先算渲染范围、做视锥判断，
 * 之后才看渲染器是不是透明渲染器。绝大多数方块实体不是，渲染范围白算（Forge 默认范围要查碰撞形状）。
 * 这里先按渲染器表判断类型，不是透明渲染器的直接返回哨兵范围，视锥判断见到哨兵即视为不可见。
 * 透明渲染器的方块实体仍按原顺序算范围、做视锥判断和取渲染器。
 */
@Mixin(value = BCClientEventHandler.class, remap = false)
public abstract class BCTransparentPassMixin {
    @Unique
    private static final AABB SDBF$NOT_TRANSPARENT = new AABB(0, 0, 0, 0, 0, 0);

    @Redirect(method = "renderLevelStage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;getRenderBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private AABB sdbf$boundsForTransparentOnly(BlockEntity tile) {
        if (ClientPerformanceOptions.draconicTransparentPass
                && !(((BlockEntityRenderDispatcherAccessor) Minecraft.getInstance().getBlockEntityRenderDispatcher())
                        .sdbf$renderers().get(tile.getType()) instanceof BlockEntityRendererTransparent<?>)) {
            return SDBF$NOT_TRANSPARENT;
        }
        return tile.getRenderBoundingBox();
    }

    @Redirect(method = "renderLevelStage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z", remap = true))
    private boolean sdbf$skipNonTransparent(Frustum frustum, AABB bounds) {
        return bounds != SDBF$NOT_TRANSPARENT && frustum.isVisible(bounds);
    }
}
