package com.tonywww.slashblade_sendims.compat.jei;

import com.tonywww.slashblade_sendims.SenDims;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class DrawableBackGround implements IDrawable {
    public static DrawableBackGround INSTANCE = new DrawableBackGround();
    public static ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(SenDims.MOD_ID,"textures/gui/stacking.png");

    @Override
    public int getWidth() {
        return 128;
    }

    @Override
    public int getHeight() {
        return 64;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
        guiGraphics.blit(
                TEXTURE,
                0,0,
                0,0,
                128,64,
                256,256
        );
    }
}
