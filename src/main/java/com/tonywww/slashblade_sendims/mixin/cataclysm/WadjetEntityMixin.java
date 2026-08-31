package com.tonywww.slashblade_sendims.mixin.cataclysm;

import com.github.L_Ender.cataclysm.entity.InternalAnimationMonster.Wadjet_Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Wadjet_Entity.class)
public class WadjetEntityMixin {

    @Redirect(
            method = "canBeSeenAsEnemy",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/github/L_Ender/cataclysm/entity/InternalAnimationMonster/Wadjet_Entity;isSleep()Z",
                    remap = false
            )
    )
    private boolean sendims$allowTargetingAfterAwakening(Wadjet_Entity wadjet) {
        return !wadjet.isSleep();
    }
}