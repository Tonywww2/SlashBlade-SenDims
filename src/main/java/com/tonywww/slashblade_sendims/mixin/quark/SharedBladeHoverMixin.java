package com.tonywww.slashblade_sendims.mixin.quark;

import com.tonywww.slashblade_sendims.compat.quark.SharedBladeSnapshot;
import javax.annotation.Nullable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoverEvent.ItemStackInfo.class)
public abstract class SharedBladeHoverMixin {
    @Shadow @Final private Item item;
    @Shadow @Final private int count;
    @Shadow @Final @Nullable private CompoundTag tag;
    @Shadow @Nullable private ItemStack itemStack;

    @Inject(method = "getItemStack", at = @At("HEAD"), cancellable = true)
    private void sendims$restoreBladeSnapshot(CallbackInfoReturnable<ItemStack> cir) {
        if (this.itemStack != null || !(this.item instanceof ItemSlashBlade)) {
            return;
        }
        CompoundTag saved = SharedBladeSnapshot.readSnapshot(this.tag,
                BuiltInRegistries.ITEM.getKey(this.item).toString(), this.count);
        if (saved == null) {
            return;
        }

        // Vanilla new ItemStack + setTag drops ForgeCaps and never invokes
        // readShareTag. ItemStack.of restores all serialized capabilities.
        ItemStack restored = ItemStack.of(saved);
        if (!restored.isEmpty()) {
            this.itemStack = restored;
            cir.setReturnValue(restored);
        }
    }
}
