package com.tonywww.slashblade_sendims.client.overlay;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.sa.AmmoSA;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class AmmoSAOverlay implements IGuiOverlay {
    public static final AmmoSAOverlay INSTANCE = new AmmoSAOverlay();
    private final Minecraft minecraft = Minecraft.getInstance();
    private static final ResourceLocation HUD = SenDims.prefix("textures/gui/ammo_gui.png");

    private AmmoSAOverlay() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Player player = this.minecraft.player;
        if (player != null && !player.isSpectator()) {
            ItemStack stackInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (stackInHand.getItem() instanceof ItemSlashBlade && stackInHand.hasTag()) {
                CompoundTag nbt = stackInHand.getTag();

                if (nbt != null && nbt.contains(AmmoSA.MAX_AMMO_PATH)) {
                    int maxAmmo = nbt.getInt(AmmoSA.MAX_AMMO_PATH);
                    int currentAmmo = nbt.contains(AmmoSA.AMMO_PATH) ? nbt.getInt(AmmoSA.AMMO_PATH) : 0;

                    int iconWidth = 4;
                    int iconHeight = 8;
                    int spacing = 6;
                    int maxPerRow = 12;
                    int rowSpacing = 10;

                    int yInit = screenHeight / 2 + 15; // Just below the crosshair

                    for (int i = 0; i < maxAmmo; i++) {
                        int row = i / maxPerRow;
                        int col = i % maxPerRow;

                        int itemsInThisRow = Math.min(maxPerRow, maxAmmo - row * maxPerRow);
                        int totalWidth = itemsInThisRow * spacing - (spacing - iconWidth);
                        int xInit = (screenWidth - totalWidth) / 2;

                        int xStart = xInit + col * spacing;
                        int yStart = yInit + row * rowSpacing;

                        // Draw empty shell
                        guiGraphics.blit(HUD, xStart, yStart,
                                0, 0, iconWidth, iconHeight, 8, 8);

                        // Draw full ammo if current > i
                        if (currentAmmo > i) {
                            guiGraphics.blit(HUD, xStart, yStart,
                                    iconWidth, 0, iconWidth, iconHeight, 8, 8);
                        }
                    }
                }
            }
        }
    }
}
