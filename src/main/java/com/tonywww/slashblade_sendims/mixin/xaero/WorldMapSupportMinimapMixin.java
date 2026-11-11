package com.tonywww.slashblade_sendims.mixin.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 世界地图向小地图取洞穴起始高度时，小地图不在洞穴模式就回退到手动起始高度；这里同样按"自动"处理。 */
@Pseudo
@Mixin(targets = "xaero.map.mods.SupportXaeroMinimap", remap = false)
public abstract class WorldMapSupportMinimapMixin {
    @Redirect(method = "getCaveStart", at = @At(value = "INVOKE", target = "Ljava/lang/Integer;intValue()I", ordinal = 0), remap = false)
    private int sdbf$ignoreManualCaveStart(Integer manualStart) {
        return Integer.MAX_VALUE;
    }
}
