package com.tonywww.slashblade_sendims.mixin.apotheosis;

import com.tonywww.slashblade_sendims.SBSDValues;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.GemInstance;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.cutting.GemCuttingMenu;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GemCuttingMenu.RarityUpgrade.class, remap = false)
public class GemCuttingRarityUpgradeMixin {

    /**
     * 修改匹配逻辑，只允许使用高级材料（Next Rarity Material）进行升级。
     * 并且要求数量至少为 2。
     */
    @Inject(method = "matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void onMatches(ItemStack gem, ItemStack left, ItemStack bot, ItemStack right, CallbackInfoReturnable<Boolean> cir) {
        GemInstance g = GemInstance.unsocketed(gem);
        GemInstance g2 = GemInstance.unsocketed(bot);

        // 基本校验（逻辑参考原版）
        if (!g.isValidUnsocketed() || !g2.isValidUnsocketed() || g.gem() != g2.gem() || g.rarity() != g2.rarity())
            return;
        if (g.isMaxRarity()) return;

        // 校验粉末成本（使用原版静态方法保证一致性）
        if (left.getItem() != dev.shadowsoffire.apotheosis.adventure.Adventure.Items.GEM_DUST.get()
                || left.getCount() < GemCuttingMenu.getDustCost(g.rarity().get())) {
            cir.setReturnValue(false);
            return;
        }

        if (!RarityRegistry.isMaterial(right.getItem())) return;

        DynamicHolder<LootRarity> matRarity = RarityRegistry.getMaterialRarity(right.getItem());
        DynamicHolder<LootRarity> gemRarity = g.rarity();

        // 核心修改：只允许下一级材料 (Next Rarity)，且数量要求为 2
        if (matRarity == RarityRegistry.next(gemRarity)) {
            cir.setReturnValue(right.getCount() >= SBSDValues.GEM_UPGRADE_COST);
        } else {
            cir.setReturnValue(false);
        }
    }

    /**
     * 修改消耗逻辑，配合 matches 的修改，扣除 2 个辅助材料。
     */
    @Inject(method = "decrementInputs(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
    private void onDecrementInputs(ItemStack gem, ItemStack left, ItemStack bot, ItemStack right, CallbackInfo ci) {
        DynamicHolder<LootRarity> gemRarity = GemInstance.unsocketed(gem).rarity();

        gem.shrink(1);
        left.shrink(GemCuttingMenu.getDustCost(gemRarity.get()));
        bot.shrink(1);

        // 始终消耗 2 个（因为 matches 保证了只有高级材料能通过且数量 >= 2）
        right.shrink(SBSDValues.GEM_UPGRADE_COST);

        ci.cancel();
    }
}

