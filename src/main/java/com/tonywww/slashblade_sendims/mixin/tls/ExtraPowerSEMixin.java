package com.tonywww.slashblade_sendims.mixin.tls;

import cn.mmf.tls.se.ExtraPowerSE;
import cn.mmf.tls.se.TLSSpecialEffectRegistry;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ExtraPowerSE.class, remap = false)
public class ExtraPowerSEMixin {

    @Redirect(method = "setEffect", at = @At(value = "INVOKE", target = "Lmods/flammpfeil/slashblade/event/SlashBladeEvent$DoSlashEvent;setDamage(D)V"))
    private static void onSetDamage(SlashBladeEvent.DoSlashEvent instance, double damage,
                                   SlashBladeEvent.DoSlashEvent event, ISlashBladeState state,
                                   RegistryObject<ExtraPowerSE> se) {
        // Both power SEs use their own coefficient without the original SE-level multiplier.
        if (se == TLSSpecialEffectRegistry.BEWITCHED_POWER
                || se == TLSSpecialEffectRegistry.EXTRA_POWER) {
            double coefficient = se == TLSSpecialEffectRegistry.EXTRA_POWER ? 0.004D : 0.008D;
            damage = instance.getDamage()
                    + event.getBlade().getEnchantmentLevel(Enchantments.POWER_ARROWS) * coefficient;
        }
        instance.setDamage(damage);
    }
}
