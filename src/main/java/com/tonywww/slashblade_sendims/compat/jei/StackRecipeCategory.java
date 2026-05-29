package com.tonywww.slashblade_sendims.compat.jei;

import com.tonywww.slashblade_sendims.SenDims;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class StackRecipeCategory extends AbstractRecipeCategory<StackData> {
    public static RecipeType<StackData> stackDataRecipeType = RecipeType.create(SenDims.MOD_ID, "stack_data", StackData.class);

    public StackRecipeCategory(IGuiHelper guiHelper) {
        super(
                stackDataRecipeType,
                Component.literal("aaa"),
                guiHelper.createDrawableItemLike(Items.PAPER),
                128, 64
        );
    }

    @SuppressWarnings("removal")
    @Override
    public @Nullable IDrawable getBackground() {
        return DrawableBackGround.INSTANCE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StackData recipe, IFocusGroup focuses) {
        builder.addInputSlot(25, 5)
                .addItemStacks(recipe.putItem());
        builder.addInputSlot(5, 42)
                .addItemStack(recipe.targetItem());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, StackData recipe, IFocusGroup focuses) {
        builder.addText(Component.translatable(recipe.lang()),getWidth()-60,64)
                .setPosition(60,0);
    }
}
