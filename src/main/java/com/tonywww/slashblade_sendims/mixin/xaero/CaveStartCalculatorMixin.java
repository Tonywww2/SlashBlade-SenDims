package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.compat.xaero.XaeroCaveMode;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 世界地图自己检测洞穴起始高度时（小地图隐藏等情况）：沿用 Xaero 原逻辑，自动洞穴模式固定为默认值；
 * 部分层级档在检测不到遮挡时与小地图手动洞穴模式一样取脚下 +4。
 */
@Pseudo
@Mixin(targets = "xaero.map.misc.CaveStartCalculator", remap = false)
public abstract class CaveStartCalculatorMixin {
    @Redirect(method = "getCaving", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 0), remap = false)
    private int sdbf$defaultAutoCaveMode(Integer configured) {
        return XaeroCaveMode.WORLD_MAP_AUTO_CAVE_MODE;
    }

    @Inject(method = "getCaving", at = @At("RETURN"), cancellable = true, remap = false)
    private void sdbf$layeredWithoutCeiling(double x, double y, double z, Level level, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == Integer.MAX_VALUE && XaeroCaveMode.worldMapCaveType() == XaeroCaveMode.LAYERED) {
            cir.setReturnValue(XaeroCaveMode.manualTop(y));
        }
    }
}
