package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.compat.xaero.XaeroCaveMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 世界地图设置页去掉洞穴模式选项（另外两个构造器都委托到这一个）。 */
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiWorldMapSettings", remap = false)
public abstract class WorldMapSettingsScreenMixin {
    @Inject(method = "<init>(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/Screen;Lxaero/lib/client/gui/config/context/IEditConfigScreenContext;)V",
            at = @At("TAIL"), remap = false)
    private void sdbf$hideCaveSettings(CallbackInfo ci) {
        XaeroCaveMode.hideCaveSettings(this);
    }
}
