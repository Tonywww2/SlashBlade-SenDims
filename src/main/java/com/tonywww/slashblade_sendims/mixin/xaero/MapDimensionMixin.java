package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.compat.xaero.XaeroCaveMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 世界地图每个维度的洞穴档位：0 = 普通（自动），1 = 部分层级，去掉"完全"。
 * 计算洞穴起始高度期间两档都按分层处理，普通档的起始高度由自动检测决定（没有遮挡时就是地表）。
 */
@Pseudo
@Mixin(targets = "xaero.map.world.MapDimension", remap = false)
public abstract class MapDimensionMixin {
    @Shadow(remap = false)
    private int caveModeType;

    /**
     * 进入世界时（本次会话第一次载入某个维度的世界地图配置后），不论存档里是"部分层级"还是"完全"等旧档位，一律回到普通（自动）。
     * 之后玩家在本次会话里切换的档位照常生效。
     */
    @Inject(method = "onCreationUnsynced", at = @At("RETURN"), remap = false)
    private void sdbf$startInNormalMode(CallbackInfo ci) {
        this.caveModeType = XaeroCaveMode.OFF;
    }

    @Inject(method = "getCaveModeType", at = @At("RETURN"), cancellable = true, remap = false)
    private void sdbf$normalOrLayered(CallbackInfoReturnable<Integer> cir) {
        if (XaeroCaveMode.isUpdatingCaveStart() || cir.getReturnValueI() > XaeroCaveMode.LAYERED) {
            cir.setReturnValue(XaeroCaveMode.LAYERED);
        }
    }

    @Inject(method = "toggleCaveModeType", at = @At("HEAD"), cancellable = true, remap = false)
    private void sdbf$toggleNormalAndLayered(CallbackInfo ci) {
        this.caveModeType = this.caveModeType == XaeroCaveMode.OFF ? XaeroCaveMode.LAYERED : XaeroCaveMode.OFF;
        ci.cancel();
    }
}
