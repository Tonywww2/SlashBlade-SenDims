package com.tonywww.slashblade_sendims.mixin;

import com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.Ignited_Berserker_Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Ignited_Berserker_Entity.class)
public class MixinIgnitedBerserkerEntity {

    @Inject(at = @At("HEAD"), method = "aiStep", remap = false, cancellable = true)
    public void slashBlade_SenDims$aiStep(CallbackInfo ci) {

    }

}
