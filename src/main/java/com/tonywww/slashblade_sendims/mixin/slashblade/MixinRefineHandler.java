package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.RefineSettlementEvent;
import mods.flammpfeil.slashblade.event.handler.RefineHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RefineHandler.class, remap = false)
public abstract class MixinRefineHandler {

    @ModifyConstant(method = "onAnvilUpdateEvent", constant = @Constant(intValue = 10, ordinal = 0))
    private int modifyRefineLimit(int constant) {
        return 5;
    }

    @ModifyConstant(method = "refineLimitCheck", constant = @Constant(intValue = 10, ordinal = 0))
    private int modifyRefineLimitCheck(int constant) {
        return 5;
    }

    @Inject(
            method = "getRefineProudsoulCount(ILmods/flammpfeil/slashblade/capability/slashblade/ISlashBladeState;Lmods/flammpfeil/slashblade/event/RefineSettlementEvent;)I",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void modifyRefineProudsoulCount(int level, ISlashBladeState state, RefineSettlementEvent e2, CallbackInfoReturnable<Integer> cir) {
        int refineDiff = e2.getRefineResult() - state.getRefine();
        // x ^ 2
        int refineSoul = refineDiff * level * 5;
        // x
        int baseSoul = e2.getMaterialCost() * 75;

        int totalSoul = baseSoul + refineSoul;

        cir.setReturnValue(totalSoul);
    }
}

