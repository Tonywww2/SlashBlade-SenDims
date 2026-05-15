package com.tonywww.slashblade_sendims.mixin.apotheosis;

import com.tonywww.slashblade_sendims.SBSDValues;

import dev.shadowsoffire.apotheosis.adventure.client.AdventureContainerScreen;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.cutting.GemCuttingMenu;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.cutting.GemCuttingScreen;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = GemCuttingScreen.class, remap = false)
public abstract class GemCuttingScreenMixin extends AdventureContainerScreen<GemCuttingMenu> {

    public GemCuttingScreenMixin(GemCuttingMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Shadow
    protected abstract void addMatTooltip(DynamicHolder<LootRarity> rarity, int cost, List<Component> list);

    /**
     * 修改渲染工具提示的逻辑，使其仅显示“高级材料”且数量为 2。
     */
    @Inject(method = { "renderTooltip", "m_280072_" }, at = @At("HEAD"), cancellable = true, remap = false)
    private void onRenderTooltip(GuiGraphics gfx, int pX, int pY, CallbackInfo ci) {
        GemCuttingScreen screen = (GemCuttingScreen) (Object) this;
        ItemStack gemStack = screen.getMenu().getSlot(0).getItem();
        GemInstance gem = GemInstance.unsocketed(gemStack);
        GemInstance secondary = GemInstance.unsocketed(screen.getMenu().getSlot(2).getItem());
        List<Component> list = new ArrayList<>();

        if (gem.isValidUnsocketed()) {
            int dust = screen.getMenu().getSlot(1).getItem().getCount();
            DynamicHolder<LootRarity> rarity = gem.rarity();

            if (rarity == RarityRegistry.getMaxRarity()) {
                list.add(Component.translatable("text.apotheosis.no_upgrade").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
            } else {
                list.add(Component.translatable("text.apotheosis.cut_cost").withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE));
                list.add(CommonComponents.EMPTY);

                int dustCost = GemCuttingMenu.getDustCost(rarity.get());
                boolean hasDust = dust >= dustCost;
                list.add(Component.translatable("text.apotheosis.cost", dustCost, dev.shadowsoffire.apotheosis.adventure.Adventure.Items.GEM_DUST.get().getName(ItemStack.EMPTY))
                        .withStyle(hasDust ? ChatFormatting.GREEN : ChatFormatting.RED));

                boolean hasGem2 = secondary.isValidUnsocketed() && gem.gem() == secondary.gem() && rarity == secondary.rarity();
                list.add(Component.translatable("text.apotheosis.cost", 1, gemStack.getHoverName().getString()).withStyle(hasGem2 ? ChatFormatting.GREEN : ChatFormatting.RED));

                list.add(Component.translatable("text.apotheosis.one_rarity_mat").withStyle(ChatFormatting.GRAY));
                this.addMatTooltip(RarityRegistry.next(rarity), SBSDValues.GEM_UPGRADE_COST, list);
            }
        }

        this.drawOnLeft(gfx, list, screen.getGuiTop() + 16);
        super.renderTooltip(gfx, pX, pY);
        ci.cancel();
    }
}
