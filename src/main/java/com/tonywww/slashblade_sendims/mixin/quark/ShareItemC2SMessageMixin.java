package com.tonywww.slashblade_sendims.mixin.quark;

import com.tonywww.slashblade_sendims.compat.quark.SharedBladeSnapshot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.violetmoon.quark.base.network.message.ShareItemC2SMessage", remap = false)
public abstract class ShareItemC2SMessageMixin {
    @Shadow public ItemStack toShare;

    @Inject(method = "<init>(Lnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"), remap = false)
    private void sendims$snapshotBlade(ItemStack stack, CallbackInfo ci) {
        this.toShare = SharedBladeSnapshot.forSharing(this.toShare);
    }
}
