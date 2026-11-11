package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.compat.xaero.XaeroCaveMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 小地图"手动洞穴模式"快捷键改为切换世界地图当前维度的档位（普通（自动）/ 部分层级）；
 * "手动洞穴模式"状态（信息显示等处使用）对应部分层级。
 */
@Pseudo
@Mixin(targets = "xaero.common.minimap.MinimapProcessor", remap = false)
public abstract class MinimapProcessorMixin {
    @Inject(method = "toggleManualCaveMode", at = @At("HEAD"), cancellable = true, remap = false)
    private void sdbf$toggleWorldMapCaveMode(CallbackInfo ci) {
        if (XaeroCaveMode.toggleFromKey()) ci.cancel();
    }

    @Inject(method = "isManualCaveMode", at = @At("RETURN"), cancellable = true, remap = false)
    private void sdbf$layeredIsManual(CallbackInfoReturnable<Boolean> cir) {
        int type = XaeroCaveMode.worldMapCaveType();
        if (type != XaeroCaveMode.NO_WORLD_MAP) cir.setReturnValue(type == XaeroCaveMode.LAYERED);
    }
}
