package com.tonywww.slashblade_sendims.mixin.aether_redux;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.zepalesque.redux.item.accessory.ConstructionRingItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;

@Mixin(ConstructionRingItem.class)
public class ConstructionRingItemMixin {

    @Inject(
            method = "getAttributeModifiers(Ltop/theillusivec4/curios/api/SlotContext;Ljava/util/UUID;Lnet/minecraft/world/item/ItemStack;)Lcom/google/common/collect/Multimap;",
            at = @At("HEAD"),
            remap = false,
            cancellable = true
    )
    private void injectGetAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();
        attributes.put((Attribute) ForgeMod.BLOCK_REACH.get(), new AttributeModifier(uuid, "Valkyrie Ring Block Reach Boost", 2.75, AttributeModifier.Operation.ADDITION));
        attributes.put((Attribute) ForgeMod.ENTITY_REACH.get(), new AttributeModifier(uuid, "Valkyrie Ring Entity Reach Boost", 0.25, AttributeModifier.Operation.ADDITION));
        cir.setReturnValue(attributes);
        cir.cancel();
    }
}
