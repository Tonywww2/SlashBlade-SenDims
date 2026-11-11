package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.compat.placebo.StoredEnchantmentScope;
import mods.flammpfeil.slashblade.recipe.SlashBladeShapedRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 拔刀剑有序合成把原刀附魔并入成品时，只转移存储的附魔，不把宝石、Tetra 材料等临时等级写进成品。
 * 包住整个 updateEnchantment 调用，因此其他模组替换该方法内容（如 slashblade_useful_addon）时同样生效。
 */
@Mixin(value = SlashBladeShapedRecipe.class, remap = false)
public abstract class SlashBladeShapedRecipeMixin {

    @Shadow
    private void updateEnchantment(ItemStack result, ItemStack ingredient) {
        throw new AssertionError();
    }

    // m_5874_ 为 assemble 的正式环境混淆名
    @Redirect(
            method = {
                    "assemble(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",
                    "m_5874_(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lmods/flammpfeil/slashblade/recipe/SlashBladeShapedRecipe;updateEnchantment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
                    remap = false
            ),
            remap = false
    )
    private void sendims$updateStoredEnchantments(SlashBladeShapedRecipe recipe, ItemStack result, ItemStack ingredient) {
        StoredEnchantmentScope.run(() -> this.updateEnchantment(result, ingredient));
    }
}
