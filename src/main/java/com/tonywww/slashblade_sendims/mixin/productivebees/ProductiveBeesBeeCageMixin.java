package com.tonywww.slashblade_sendims.mixin.productivebees;

import cy.jdkdigital.productivebees.common.item.BeeCage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BeeCage.class, remap = false)
public class ProductiveBeesBeeCageMixin {

    @Inject(method = "captureEntity", at = @At("TAIL"))
    private static void onCaptureEntity(Bee target, ItemStack cageStack, CallbackInfo ci) {
        CompoundTag nbt = cageStack.getTag();
        if (nbt != null && !target.hasCustomName()) {
            if (target instanceof cy.jdkdigital.productivebees.common.entity.bee.ConfigurableBee configurableBee) {
                nbt.putString("name", "entity.productivebees." + configurableBee.getBeeName() + "_bee");
            } else {
                nbt.putString("name", target.getType().getDescriptionId());
            }
            nbt.putBoolean("is_localized_name", true);
        }
    }

    @Inject(method = { "getName", "m_7626_", "getName(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/chat/Component;" }, at = @At("HEAD"), cancellable = true, remap = false)
    private void onGetName(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        if (BeeCage.isFilled(stack)) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.getBoolean("is_localized_name")) {
                String entityId = tag.getString("name");
                BeeCage cage = (BeeCage) (Object) this;
                cir.setReturnValue(Component.translatable(cage.getDescriptionId())
                        .append(Component.literal(" ("))
                        .append(Component.translatable(entityId))
                        .append(Component.literal(")")));
            }
        }
    }
}
