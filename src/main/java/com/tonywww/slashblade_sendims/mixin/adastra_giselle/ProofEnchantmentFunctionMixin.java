package com.tonywww.slashblade_sendims.mixin.adastra_giselle;

import ad_astra_giselle_addon.common.content.proof.ProofEnchantmentFunction;
import ad_astra_giselle_addon.common.enchantment.EnchantmentHelper2;
import ad_astra_giselle_addon.common.entity.LivingHelper;
import ad_astra_giselle_addon.common.item.ItemStackReference;
import ad_astra_giselle_addon.common.item.ItemUsableResource;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes the resource selection of proof enchantments (oxygen / gravity / hot temperature / acid rain proof).
 *
 * <p>The original {@link ProofEnchantmentFunction#provide(Entity)} only ever considers a single
 * resource returned by {@code ItemUsableResource.first(stack)}. Because {@code Energy} is tested
 * before {@code Durability}, any energy-capable item always resolves to {@code Energy}. When such an
 * item cannot supply enough energy, the simulated {@code consume} fails, the whole branch is skipped
 * and the method returns {@code 0} &mdash; the proof never falls back to spending durability even
 * though the item could pay with it.
 *
 * <p>This mixin re-runs the selection over <em>all</em> applicable {@link ItemUsableResource}s and
 * uses the first one that can actually be paid, so an energy item with empty/insufficient energy now
 * correctly falls back to consuming durability.
 */
@Mixin(value = ProofEnchantmentFunction.class, remap = false)
public abstract class ProofEnchantmentFunctionMixin {

    @Shadow
    public abstract Enchantment getEnchantment();

    @Shadow
    public abstract boolean consume(LivingEntity living, EquipmentSlot slot, ItemStackReference enchantedItem, ItemUsableResource resource, boolean simulate);

    @Shadow
    public abstract int getProofDuration(ItemUsableResource resource);

    @Inject(method = "provide", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$fallbackToDurabilityWhenResourceUnavailable(Entity entity, CallbackInfoReturnable<Integer> cir) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        Pair<EquipmentSlot, Integer> pair = EnchantmentHelper2.getEnchantmentItemAndLevel(this.getEnchantment(), living);
        EquipmentSlot slot = pair.getFirst();
        int enchantLevel = pair.getSecond();
        if (slot == null || enchantLevel == 0) {
            cir.setReturnValue(0);
            return;
        }

        if (!LivingHelper.isPlayingMode(living)) {
            // Matches the original behaviour: ProofAbstractUtils.GENERAL_PROOF_INTERVAL.
            cir.setReturnValue(10);
            return;
        }

        ItemStackReference enchantedItem = LivingHelper.getEquipmentItem(living, slot);
        ItemStack stack = enchantedItem.getStack();

        // Try every applicable resource (Energy, then Durability) instead of only the first match,
        // so an energy item that cannot pay with energy still falls back to durability.
        for (ItemUsableResource resource : ItemUsableResource.values()) {
            if (resource.test(stack) && this.consume(living, slot, enchantedItem, resource, true)) {
                this.consume(living, slot, enchantedItem, resource, false);
                cir.setReturnValue(this.getProofDuration(resource));
                return;
            }
        }

        cir.setReturnValue(0);
    }
}
