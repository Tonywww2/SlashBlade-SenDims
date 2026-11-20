package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.client.zenith.ZenithRiftBlade;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 天顶·裂空的发光分组改为采样着色器画布：拔刀剑所有发光材质（手持、腰间、物品、掉落物、刀架）都从这三个方法取，
 * 只有该模型的贴图会被替换，其他刀不受影响。
 */
@Mixin(value = BladeRenderState.class, remap = false)
public abstract class BladeRenderStateRiftMixin {
    @ModifyVariable(method = {"getSlashBladeBlendLuminous", "getSlashBladeBlendLuminousDepthWrite",
            "getSlashBladeBlendReverseLuminous"}, at = @At("HEAD"), argsOnly = true)
    private static ResourceLocation sendims$riftCanvas(ResourceLocation texture) {
        return ZenithRiftBlade.luminous(texture);
    }
}
