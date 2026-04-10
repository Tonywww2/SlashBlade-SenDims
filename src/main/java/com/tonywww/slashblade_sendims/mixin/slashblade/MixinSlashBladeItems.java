package com.tonywww.slashblade_sendims.mixin.slashblade;

import net.minecraft.world.item.ItemStack;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "mods.flammpfeil.slashblade.registry.SlashBladeItems$1", // PROUDSOUL
        "mods.flammpfeil.slashblade.registry.SlashBladeItems$2", // PROUDSOUL_INGOT
        "mods.flammpfeil.slashblade.registry.SlashBladeItems$3", // PROUDSOUL_TINY
        "mods.flammpfeil.slashblade.registry.SlashBladeItems$4", // PROUDSOUL_SPHERE
        "mods.flammpfeil.slashblade.registry.SlashBladeItems$5"  // PROUDSOUL_CRYSTAL
})
public abstract class MixinSlashBladeItems {

    @Inject(method = "getEnchantmentValue(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true, remap = false)
    public void modifyEnchantmentValue(ItemStack stack, CallbackInfoReturnable<Integer> cir) {

        if (stack.is(SlashBladeItems.PROUDSOUL_TINY.get())) {
            cir.setReturnValue(5);
        } else if (stack.is(SlashBladeItems.PROUDSOUL.get())) {
            cir.setReturnValue(10);
        } else if (stack.is(SlashBladeItems.PROUDSOUL_INGOT.get())) {
            cir.setReturnValue(25);
        } else if (stack.is(SlashBladeItems.PROUDSOUL_SPHERE.get())) {
            cir.setReturnValue(50);
        } else if (stack.is(SlashBladeItems.PROUDSOUL_CRYSTAL.get())) {
            cir.setReturnValue(100);
        }
    }
}
