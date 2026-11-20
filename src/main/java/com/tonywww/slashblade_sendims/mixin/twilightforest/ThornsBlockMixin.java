package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.twilight.TwilightProgression;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.block.ThornsBlock;

/**
 * Thorns break normally instead of bursting into more thorns, so players can clear thorns blocking the Final Castle
 * by hand. Contact damage and the missing drops are unchanged; hardness is set by the modpack.
 */
@Mixin(value = ThornsBlock.class, remap = false)
public abstract class ThornsBlockMixin {
    @Inject(method = "onDestroyedByPlayer", at = @At("HEAD"), cancellable = true)
    private void sendims$breakNormally(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
                                       FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if (!TwilightProgression.thornsBreakable() || player.getAbilities().instabuild) {
            return;
        }
        state.getBlock().playerWillDestroy(level, pos, state, player);
        cir.setReturnValue(level.setBlock(pos, fluid.createLegacyBlock(), level.isClientSide() ? 11 : 3));
    }
}
