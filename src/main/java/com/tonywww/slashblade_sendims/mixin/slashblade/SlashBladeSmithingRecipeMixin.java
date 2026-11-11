package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.compat.placebo.StoredEnchantmentScope;
import mods.flammpfeil.slashblade.recipe.SlashBladeSmithingRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 锻造台升级拔刀剑时同样只转移存储的附魔，见 {@link SlashBladeShapedRecipeMixin}。
 */
@Mixin(value = SlashBladeSmithingRecipe.class, remap = false)
public abstract class SlashBladeSmithingRecipeMixin {

    @Shadow
    private void updateEnchantment(ItemStack result, ItemStack ingredient) {
        throw new AssertionError();
    }

    // m_5874_ 为 assemble 的正式环境混淆名
    @Redirect(
            method = {
                    "assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
                    "m_5874_(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lmods/flammpfeil/slashblade/recipe/SlashBladeSmithingRecipe;updateEnchantment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
                    remap = false
            ),
            remap = false
    )
    private void sendims$updateStoredEnchantments(SlashBladeSmithingRecipe recipe, ItemStack result, ItemStack ingredient) {
        StoredEnchantmentScope.run(() -> this.updateEnchantment(result, ingredient));
    }
}
