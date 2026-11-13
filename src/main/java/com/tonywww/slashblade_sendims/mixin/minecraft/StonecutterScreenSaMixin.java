package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.client.SaRender;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 切石机的配方按钮只调用 renderItem，不画物品装饰，SA 耀魂宝珠上的 SA 名称因此不显示。
 * 这里在每个配方按钮画完物品后补画 SA 名称，位置与样式同物品栏（{@link SaRender#saDecorator}）。
 */
@Mixin(StonecutterScreen.class)
public abstract class StonecutterScreenSaMixin {
    @Redirect(method = "renderRecipes", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void sendims$renderRecipeWithSaName(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        if (stack.is(SlashBladeItems.PROUDSOUL_SPHERE.get())) {
            SaRender.saDecorator(graphics, Minecraft.getInstance().font, stack, x, y);
        }
    }
}
