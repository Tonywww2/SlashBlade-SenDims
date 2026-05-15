package com.tonywww.slashblade_sendims.mixin.apotheosis;

import dev.shadowsoffire.apotheosis.adventure.affix.salvaging.SalvageItem;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.color.GradientColor;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SalvageItem.class, remap = false)
public abstract class SalvageItemMixin {

    @Shadow
    @Final
    protected DynamicHolder<LootRarity> rarity;

    @Inject(method = "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void onGetName(ItemStack pStack, CallbackInfoReturnable<Component> cir) {
        if (this.rarity != null && this.rarity.getId() != null && "ancient".equals(this.rarity.getId().getPath())) {
            cir.setReturnValue(Component.translatable(((Item) (Object) this).getDescriptionId(pStack)).withStyle(s -> s.withColor(GradientColor.RAINBOW)));
        }
    }
}

