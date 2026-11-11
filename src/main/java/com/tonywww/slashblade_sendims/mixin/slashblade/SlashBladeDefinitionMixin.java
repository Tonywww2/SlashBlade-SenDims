package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.se.BladeDefinitionMigration;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blades created from the current definitions already match it; mark them so migration skips them. */
@Mixin(value = SlashBladeDefinition.class, remap = false)
public abstract class SlashBladeDefinitionMixin {
    @Inject(method = "getBlade(Lnet/minecraft/world/item/Item;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
    private void sendims$stampRevision(Item item, CallbackInfoReturnable<ItemStack> cir) {
        BladeDefinitionMigration.stamp(cir.getReturnValue(), ((SlashBladeDefinition) (Object) this).getTranslationKey());
    }
}
