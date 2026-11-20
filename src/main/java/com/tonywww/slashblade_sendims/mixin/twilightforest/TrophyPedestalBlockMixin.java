package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.twilight.TwilightProgression;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.block.TrophyPedestalBlock;

/** The pedestal hard-codes progress_lich; the reworked order reaches the knights before the lich. */
@Mixin(value = TrophyPedestalBlock.class, remap = false)
public abstract class TrophyPedestalBlockMixin {
    @Inject(method = "isPlayerEligible", at = @At("HEAD"), cancellable = true)
    private void sendims$configuredAdvancement(Player player, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(TwilightProgression.isPedestalEligible(player));
    }
}
