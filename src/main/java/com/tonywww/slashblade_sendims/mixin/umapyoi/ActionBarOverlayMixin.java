package com.tonywww.slashblade_sendims.mixin.umapyoi;

import com.tonywww.slashblade_sendims.client.RenderAmount;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.client.ActionBarOverlay;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = ActionBarOverlay.class, remap = false)
public class ActionBarOverlayMixin {
    @Shadow
    @Final
    private static ResourceLocation HUD;

    @Shadow
    @Final
    private Minecraft minecraft;

    private int apCache = 0;
    private List<RenderAmount> renderAmoumt = new ObjectArrayList<>();

    /**
     * @author _yi_ran_
     * @reason render amount
     */
    @Overwrite
    private void renderSkill(ItemStack soul, GuiGraphics guiGraphics, int x, int y) {
        int ap = UmaSoulUtils.getActionPoint(soul);
        int maxAp = UmaSoulUtils.getMaxActionPoint(soul);
        guiGraphics.blit(HUD, x, y - 128, 11.0F, 0.0F, 5, 128, 16, 128);
        int apbar = ap != 0 ? ap * 128 / maxAp : 0;

        if (ap < apCache) {
            renderAmoumt.add(new RenderAmount(x - 8, y - apbar - 30, String.valueOf(ap - apCache)));
        }
        apCache = ap;
        renderAmoumt.removeIf(renderAmount -> renderAmount.render(guiGraphics, this.minecraft.font));
        guiGraphics.blit(HUD, x, y - apbar, 6.0F, (float) (128 - apbar), 5, apbar, 16, 128);
        guiGraphics.blit(HUD, x - 7, y - 3 - apbar, 0.0F, 0.0F, 6, 7, 16, 128);
        String str = String.valueOf(ap);
        guiGraphics.drawString(this.minecraft.font, str, x - 8 - this.minecraft.font.width(str), y - 3 - apbar, 16777215);

    }
}
