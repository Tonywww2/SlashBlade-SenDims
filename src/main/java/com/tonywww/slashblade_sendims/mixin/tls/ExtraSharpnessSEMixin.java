package com.tonywww.slashblade_sendims.mixin.tls;

import cn.mmf.tls.se.ExtraSharpnessSE;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ExtraSharpnessSE.class, remap = false)
public class ExtraSharpnessSEMixin {

    @Redirect(method = "setEffect", at = @At(value = "INVOKE", target = "Lmods/flammpfeil/slashblade/event/SlashBladeEvent$DoSlashEvent;setDamage(D)V"))
    private static void onSetDamage(SlashBladeEvent.DoSlashEvent instance, double damage) {
        // Both sharpness SEs use the same coefficient, without the original SE-level multiplier.
        instance.setDamage(instance.getDamage()
                + instance.getBlade().getEnchantmentLevel(Enchantments.SHARPNESS) * 0.008D);
    }
}
