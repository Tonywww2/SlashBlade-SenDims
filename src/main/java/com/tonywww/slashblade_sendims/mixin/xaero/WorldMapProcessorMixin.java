package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.compat.xaero.XaeroCaveMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 世界地图：忽略手动设置的洞穴起始高度，自动洞穴模式和洞穴深度固定为 Xaero 默认值，普通档也参与自动洞穴检测。 */
@Pseudo
@Mixin(targets = "xaero.map.MapProcessor", remap = false)
public abstract class WorldMapProcessorMixin {
    @Shadow(remap = false)
    private int caveModeDepthConfig;

    @Inject(method = "updateCaveStart", at = @At("HEAD"), remap = false)
    private void sdbf$beginCaveStart(CallbackInfo ci) {
        XaeroCaveMode.beginCaveStartUpdate();
    }

    @Inject(method = "updateCaveStart", at = @At("RETURN"), remap = false)
    private void sdbf$endCaveStart(CallbackInfo ci) {
        XaeroCaveMode.endCaveStartUpdate();
    }

    /** updateCaveStart 里第一个 intValue 是读取 cave_mode_start_y，按"自动"处理。 */
    @Redirect(method = "updateCaveStart", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 0), remap = false)
    private int sdbf$ignoreManualCaveStart(Integer manualStart) {
        return Integer.MAX_VALUE;
    }

    /** 第二个 intValue 读取世界地图的自动洞穴模式。 */
    @Redirect(method = "updateCaveStart", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 1), remap = false)
    private int sdbf$defaultAutoCaveMode(Integer configured) {
        return XaeroCaveMode.WORLD_MAP_AUTO_CAVE_MODE;
    }

    @Inject(method = "updateConfigValuesForMultipleThreads", at = @At("TAIL"), remap = false)
    private void sdbf$defaultDepth(CallbackInfo ci) {
        this.caveModeDepthConfig = XaeroCaveMode.CAVE_DEPTH;
    }
}
