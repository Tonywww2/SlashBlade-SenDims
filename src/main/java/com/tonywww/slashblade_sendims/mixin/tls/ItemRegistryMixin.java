package com.tonywww.slashblade_sendims.mixin.tls;

import cn.mmf.tls.item.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "cn.mmf.tls.item.ItemRegistry$1", // SAKURA
        "cn.mmf.tls.item.ItemRegistry$2"  // SAKURA_FULL
})
public abstract class ItemRegistryMixin {

    @Inject(method = "getEnchantmentValue(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"), cancellable = true, remap = false)
    public void modifyEnchantmentValue(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.is(ItemRegistry.SAKURA.get())) {
            cir.setReturnValue(10);
        } else if (stack.is(ItemRegistry.SAKURA_FULL.get())) {
            cir.setReturnValue(20);
        }
    }
}

