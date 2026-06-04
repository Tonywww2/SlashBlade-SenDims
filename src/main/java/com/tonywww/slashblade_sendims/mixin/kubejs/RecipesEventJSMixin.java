package com.tonywww.slashblade_sendims.mixin.kubejs;

import com.tonywww.slashblade_sendims.SBSDValues;
import dev.latvian.mods.kubejs.recipe.RecipeJS;
import dev.latvian.mods.kubejs.recipe.RecipesEventJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipesEventJS.class)
public class RecipesEventJSMixin {

    @Inject(
            method = "<init>()V",
            at = @At("TAIL"),
            remap = false
    )
    private void onRecipesKubeEventInit(CallbackInfo ci) {
        // 在 RecipesKubeEvent 初始化完成后清空过滤器
        SBSDValues.REMOVED_RECIPE_IDS.clear();
    }

    //@Inject(
    //        method = "addRecipe(Ldev/latvian/mods/kubejs/recipe/RecipeJS;Z)Ldev/latvian/mods/kubejs/recipe/RecipeJS;",
    //        at = @At("HEAD"),
    //        remap = false
    //)
    //private void filterAddedRecipe(RecipeJS recipe, boolean json, CallbackInfoReturnable<RecipeJS> cir) {
    //    // 检查配方 ID 是否在移除列表中
    //    ResourceLocation recipeId = recipe.getOrCreateId();
    //    System.out.println(recipeId);
    //    System.out.println(SBSDValues.REMOVED_RECIPE_IDS.contains(recipeId));
    //    if (SBSDValues.REMOVED_RECIPE_IDS.contains(recipeId)) {
    //        // 标记为已移除
    //        recipe.remove();
    //    }
    //}

    @Inject(
            method = "createRecipe(Ldev/latvian/mods/kubejs/recipe/RecipeJS;)Lnet/minecraft/world/item/crafting/Recipe;",
            at = @At("HEAD"),
            remap = false
    )
    private void filterCreatedRecipe(RecipeJS recipe, CallbackInfoReturnable<Recipe<?>> cir) {
        ResourceLocation recipeId = recipe.getOrCreateId();
//        System.out.println("Filtering recipe at createRecipe: " + recipeId);
//        System.out.println("Is removed: " + SBSDValues.REMOVED_RECIPE_IDS.contains(recipeId));
        if (SBSDValues.REMOVED_RECIPE_IDS.contains(recipeId)) {
            recipe.remove();
        }
    }
}
