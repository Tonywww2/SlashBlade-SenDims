package com.tonywww.slashblade_sendims.mixin.xaero;

import com.tonywww.slashblade_sendims.client.compat.xaero.XaeroCaveMode;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 世界地图洞穴模式面板：隐藏起始高度滑条和输入框，只留档位按钮，按钮显示"普通（自动）/ 部分层级"。 */
@Pseudo
@Mixin(targets = "xaero.map.gui.GuiCaveModeOptions", remap = false)
public abstract class GuiCaveModeOptionsMixin {
    @Inject(method = "createSlider", at = @At("RETURN"), remap = false)
    private void sdbf$hideStartSlider(CallbackInfoReturnable<AbstractWidget> cir) {
        sdbf$hide(cir.getReturnValue());
    }

    @Inject(method = "createField", at = @At("RETURN"), remap = false)
    private void sdbf$hideStartField(CallbackInfoReturnable<EditBox> cir) {
        sdbf$hide(cir.getReturnValue());
    }

    @Inject(method = "setCaveStart", at = @At("HEAD"), cancellable = true, remap = false)
    private void sdbf$lockCaveStart(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "getCaveModeTypeButtonMessage", at = @At("RETURN"), cancellable = true, remap = false)
    private void sdbf$modeLabel(CallbackInfoReturnable<Component> cir) {
        Component label = XaeroCaveMode.caveModeButtonLabel(this);
        if (label != null) cir.setReturnValue(label);
    }

    @Unique
    private static void sdbf$hide(AbstractWidget widget) {
        if (widget == null) return;
        widget.visible = false;
        widget.active = false;
    }
}
