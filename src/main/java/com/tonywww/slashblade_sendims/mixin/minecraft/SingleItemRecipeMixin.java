package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class SingleItemRecipeMixin {
    @Mixin(SingleItemRecipe.Serializer.class)
    public static abstract class SerializerMixin<T extends SingleItemRecipe> implements RecipeSerializer<T> {
        @Shadow
        @Final
        SingleItemRecipe.Serializer.SingleItemMaker<T> factory;

        @Inject(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lnet/minecraft/world/item/crafting/SingleItemRecipe;", at = @At("HEAD"), cancellable = true)
        private void fromJson(ResourceLocation recipeId, JsonObject json, CallbackInfoReturnable<T> cir) {
            if (json.get("result").isJsonObject()) {
                String s = GsonHelper.getAsString(json, "group", "");
                Ingredient ingredient;
                if (GsonHelper.isArrayNode(json, "ingredient")) {
                    ingredient = Ingredient.fromJson(GsonHelper.getAsJsonArray(json, "ingredient"), false);
                } else {
                    ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"), false);
                }

                ItemStack itemstack = CraftingHelper.getItemStack(json.get("result").getAsJsonObject(), true, true);
                cir.setReturnValue(this.factory.create(recipeId, s, ingredient, itemstack));
            }
        }
    }
}
