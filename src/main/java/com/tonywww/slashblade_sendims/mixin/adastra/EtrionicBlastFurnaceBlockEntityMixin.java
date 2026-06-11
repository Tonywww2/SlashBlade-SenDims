package com.tonywww.slashblade_sendims.mixin.adastra;

import earth.terrarium.adastra.common.blockentities.machines.EtrionicBlastFurnaceBlockEntity;
import net.minecraft.world.item.crafting.BlastingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EtrionicBlastFurnaceBlockEntity.class)
public abstract class EtrionicBlastFurnaceBlockEntityMixin {

    @Inject(
            method = "craft",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void fixBlastingModeOutput(BlastingRecipe recipe, int slot, CallbackInfo ci) {
        EtrionicBlastFurnaceBlockEntity entity = (EtrionicBlastFurnaceBlockEntity) (Object) this;
        if (recipe != null && entity.getItem(slot).isEmpty()) {
            ci.cancel();
        }
    }
}
