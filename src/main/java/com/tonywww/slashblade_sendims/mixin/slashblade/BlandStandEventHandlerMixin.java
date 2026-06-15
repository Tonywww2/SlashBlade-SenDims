package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.event.bladestand.BlandStandEventHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = BlandStandEventHandler.class, remap = false)
public class BlandStandEventHandlerMixin {

    @Redirect(
            method = "eventProudSoulEnchantment",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getAllEnchantments()Ljava/util/Map;",
                    ordinal = 0,
                    remap = false
            )
    )
    private static Map<Enchantment, Integer> redirectBladeGetAllEnchantments(ItemStack blade) {
        return EnchantmentHelper.getEnchantments(blade);
    }

    @Redirect(
            method = "lambda$eventProudSoulEnchantment$1",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;canApplyAtEnchantingTable(Lnet/minecraft/world/item/enchantment/Enchantment;)Z",
                    remap = false
            )
    )
    private static boolean redirectProudSoulCanApply(ItemStack blade, Enchantment enchantment) {
        return true;
    }

}
