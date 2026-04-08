package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.event.handler.RefineHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

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
}

