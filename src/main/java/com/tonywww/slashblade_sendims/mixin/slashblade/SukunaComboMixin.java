package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.se.SukunaEffects;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ComboState.class,remap=false)
public abstract class SukunaComboMixin {
    @Shadow public abstract int getPriority();
    @Inject(method="clickAction",at=@At("HEAD"))
    private void sukunaSwing(LivingEntity user,CallbackInfo ci) {
        SukunaEffects.combo(user,ComboState.getRegistryKey((ComboState)(Object)this),getPriority());
        com.tonywww.slashblade_sendims.yakuman.YakumanEffects.combo(user,
                ComboState.getRegistryKey((ComboState)(Object)this),getPriority());
    }
}
