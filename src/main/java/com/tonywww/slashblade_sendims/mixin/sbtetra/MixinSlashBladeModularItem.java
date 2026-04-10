package com.tonywww.slashblade_sendims.mixin.sbtetra;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.yiran.sbtetra.item.ISlashBladeTetra;
import net.yiran.sbtetra.item.SlashBladeModularItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SlashBladeModularItem.class)
public abstract class MixinSlashBladeModularItem extends ItemSlashBlade implements ISlashBladeTetra {

    public MixinSlashBladeModularItem(Tier tier, int attackDamageIn, float attackSpeedIn, Properties builder) {
        super(tier, attackDamageIn, attackSpeedIn, builder);
    }

    @Inject(method = "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true, remap = true)
    private void slashblade_sendims$onGetName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        String id = this.getDescriptionId(stack);
        cir.setReturnValue(Component.literal(this.getDisplayNamePrefixes(stack)).append(Component.translatable(id)));
    }
}
