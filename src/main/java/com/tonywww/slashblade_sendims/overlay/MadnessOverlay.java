package com.tonywww.slashblade_sendims.overlay;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.se.FrenziedFlame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class MadnessOverlay implements IGuiOverlay {

    public static final MadnessOverlay INSTANCE = new MadnessOverlay();
    private final Minecraft minecraft = Minecraft.getInstance();
    private static final ResourceLocation HUD = SenDims.prefix("textures/gui/madness_bar.png");

    @Override
    public void render(ForgeGui forgeGui, GuiGraphics guiGraphics, float v, int i, int i1) {
        Player player = minecraft.player;
        if (player == null || player.isSpectator()) return;

        CompoundTag data = player.getPersistentData();

        if (data.contains(FrenziedFlame.MADNESS_PATH) && data.getInt(FrenziedFlame.MADNESS_PATH) > 0) {
            int pX = guiGraphics.guiWidth() / 2;
            int pY = guiGraphics.guiHeight() / 4 * 3;

            guiGraphics.blit(HUD, pX - 90, pY,
                    0, 0, 180, 18,
                    180, 25);

            float ratio = data.getInt(FrenziedFlame.MADNESS_PATH) / player.getMaxHealth();
            guiGraphics.blit(HUD, pX - 90 +18, pY + 6,
                    0, 18, (int) (158 * ratio), 7,
                    180, 25);


        }

    }
}
