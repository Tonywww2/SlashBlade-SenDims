package com.tonywww.slashblade_sendims.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class RenderAmount {
    public float life = 80;
    public int x;
    public int y;
    public String amount;

    public RenderAmount(int x, int y, String amount) {
        this.x = x;
        this.y = y;
        this.amount = amount;
    }

    public boolean render(GuiGraphics guiGraphics, Font font) {
        int alpha;
        if (life < 40) {
            alpha = (int) (life / 40 * 255);
        } else {
            alpha = 255;
        }
        int color = (alpha << 24) | 0x00FF0000;
        guiGraphics.drawString(font, amount, x - font.width(amount), y, color);
        return (life -= Minecraft.getInstance().getPartialTick()) <= 0.05;
    }
}
