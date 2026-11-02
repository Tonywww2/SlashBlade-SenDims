package com.tonywww.slashblade_sendims.mixin.lsp;

import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.royling.lsp.ModCap.ModCapabilities;
import net.royling.lsp.SpecialEffects.BleedingCutting;
import net.royling.lsp.SpecialEffects.SERegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BleedingCutting.class, remap = false)
public class MixinBleedingCutting {
    @Inject(method = "onSlashBladeHit", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sendims$attackBasedAccumulation(SlashBladeEvent.HitEvent event, CallbackInfo ci) {
        ci.cancel();
        if (!(event.getUser() instanceof Player player) || player.level().isClientSide()) return;
        if (!event.getSlashBladeState().hasSpecialEffect(SERegistry.BLEEDING_CUTTING.getId())) return;
        if (!SpecialEffect.isEffective(SERegistry.BLEEDING_CUTTING.get(), player.experienceLevel)) return;

        LivingEntity target = event.getTarget();
        int amount = (int) Math.floor(player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 2.0D);
        if (amount <= 0) return;
        int limit = Math.max(1, (int) Math.floor(target.getMaxHealth()));
        target.invulnerableTime = 0;
        target.getCapability(ModCapabilities.BLOOD_ACCUMULATION_CAPABILITY).ifPresent(blood -> {
            blood.setAccumulation((int) Math.min(limit, (long) blood.getAccumulation() + amount));
            blood.setLastAttackerUUID(player.getUUID());
        });
    }
}
