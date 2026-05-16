package com.tonywww.slashblade_sendims.se;

import com.github.L_Ender.cataclysm.init.ModEffect;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class ArmorMelt extends SpecialEffect {
    public ArmorMelt() {
        super(50);
    }

    public static void onHit(ServerPlayer player, LivingEntity target) {
        if (player.getRandom().nextFloat() < 0.10f) {
            target.addEffect(new MobEffectInstance(ModEffect.EFFECTBLAZING_BRAND.get(), 30, 0));
        }
    }
}

