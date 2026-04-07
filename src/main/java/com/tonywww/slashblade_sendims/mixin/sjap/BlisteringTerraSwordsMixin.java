package com.tonywww.slashblade_sendims.mixin.sjap;

import cn.mmf.slashblade_addon.compat.botania.BlisteringTerraSwords;
import cn.mmf.slashblade_addon.entity.BlisteringSwordsEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlisteringTerraSwords.class)
public class BlisteringTerraSwordsMixin {

    @Redirect(method = "lambda$doSlash$1",
              at = @At(value = "INVOKE", target = "Lcn/mmf/slashblade_addon/entity/BlisteringSwordsEntity;setDamage(D)V"),
              remap = false)
    private static void redirectSetDamage(BlisteringSwordsEntity instance, double originalDamage) {
        if (instance.getOwner() instanceof LivingEntity living) {
            double newDamage = living.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5;
            instance.setDamage(newDamage);
        } else {
            instance.setDamage(originalDamage);
        }
    }
}

