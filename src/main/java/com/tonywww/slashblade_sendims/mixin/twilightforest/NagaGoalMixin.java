package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.ai.goal.NagaMovementPattern;
import twilightforest.entity.boss.Naga;

@Mixin(value = NagaMovementPattern.class)
public class NagaGoalMixin {
    @Final
    @Shadow(remap = false)
    private Naga naga;

    @Inject(method = "doCharge(Z)V", at = @At("HEAD"), remap = false)
    public void injectDoCharge(boolean stunless, CallbackInfo ci) {
        SBSDLeader.doLeaderSATripleDrive(naga, null);
    }
}
