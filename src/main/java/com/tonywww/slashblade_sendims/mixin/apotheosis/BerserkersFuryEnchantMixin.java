package com.tonywww.slashblade_sendims.mixin.apotheosis;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.ench.enchantments.corrupted.BerserkersFuryEnchant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BerserkersFuryEnchant.class, remap = false)
public class BerserkersFuryEnchantMixin {

    @Inject(method = "livingHurt", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$onlyApplyDamageBoost(LivingHurtEvent event, CallbackInfo ci) {
        ci.cancel();

        LivingEntity user = event.getEntity();
        if (event.getSource().getEntity() instanceof Entity && user.getEffect(MobEffects.DAMAGE_RESISTANCE) == null) {
            BerserkersFuryEnchant enchantment = (BerserkersFuryEnchant) (Object) this;
            int level = EnchantmentHelper.getEnchantmentLevel(enchantment, user);
            if (level > 0) {
                if (Affix.isOnCooldown(BuiltInRegistries.ENCHANTMENT.getKey(enchantment), 900, user)) return;

                user.invulnerableTime = 0;
                DamageSource corrupted = new DamageSource(user.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(Apoth.DamageTypes.CORRUPTED));
                user.hurt(corrupted, (float) Math.pow(2.5D, level));
                user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 500, level - 1));
                Affix.startCooldown(BuiltInRegistries.ENCHANTMENT.getKey(enchantment), user);
            }
        }
    }
}
