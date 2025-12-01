package com.tonywww.slashblade_sendims.mixin.tetra;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import se.mickelus.tetra.aspect.TetraEnchantmentHelper;

@Mixin(TetraEnchantmentHelper.class)
public class TetraEnchantmentHelperMixin {
    @Inject(method = "mapEnchantment(Lnet/minecraft/world/item/ItemStack;Ljava/lang/String;Lnet/minecraft/world/item/enchantment/Enchantment;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private static void injectMapEnchantment(ItemStack itemStack, String slot, Enchantment enchantment, CallbackInfo ci){
        ci.cancel();
    }

    @Inject(method = "mapEnchantments(Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private static void injectMapEnchantments(ItemStack itemStack, CallbackInfo ci){
        ci.cancel();
    }
}
