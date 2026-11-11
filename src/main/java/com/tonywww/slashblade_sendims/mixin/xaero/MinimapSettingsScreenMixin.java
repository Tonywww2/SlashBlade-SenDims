package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.compat.xaero.XaeroCaveMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 小地图"方块地图"和"视图"设置页去掉洞穴模式选项。 */
@Pseudo
@Mixin(targets = {"xaero.common.gui.GuiMinimapBlockMapSettings", "xaero.common.gui.GuiMinimapViewSettings"}, remap = false)
public abstract class MinimapSettingsScreenMixin {
    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void sdbf$hideCaveSettings(CallbackInfo ci) {
        XaeroCaveMode.hideCaveSettings(this);
    }
}
