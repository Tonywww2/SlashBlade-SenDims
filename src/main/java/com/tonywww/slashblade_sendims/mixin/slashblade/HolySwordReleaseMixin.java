package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.se.HolySwordEffects;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import com.tonywww.slashblade_sendims.yakuman.YakumanEffects;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Successful native combo entry, after release/AP/motion cancellation and before damage. */
@Mixin(value = ComboState.class, remap = false)
public abstract class HolySwordReleaseMixin {
    @Shadow public abstract int getPriority();

    @Inject(method = "clickAction", at = @At("HEAD"))
    private void manaBurstBeforeArt(LivingEntity user, CallbackInfo ci) {
        // Native normal melee has priority > 50. This also supports changing the
        // blade's SA with an orb and native SSA, without consuming on canceled casts.
        if (getPriority() <= 50) {
            var state = NamelessSeries.getBladeState(user);
            HolySwordEffects.acceptedArt(user, state);
            YakumanEffects.acceptedNativeArt(user, state);
        }
    }
}
