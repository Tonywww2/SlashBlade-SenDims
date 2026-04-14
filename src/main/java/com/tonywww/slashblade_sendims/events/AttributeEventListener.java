package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AttributeEventListener {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource damageSource = event.getSource();

        if (damageSource.is(DamageTypes.MAGIC) || damageSource.is(DamageTypes.INDIRECT_MAGIC)) {
            LivingEntity entity = event.getEntity();
            AttributeInstance magicResistance = entity.getAttribute(SBSDAttributes.MAGIC_RESISTANCE.get());
            if (magicResistance != null) {
                double resistance = magicResistance.getValue();

                double penetration = 0.0;
                Entity attacker = damageSource.getEntity();
                if (attacker instanceof LivingEntity livingAttacker) {
                    AttributeInstance magicPenetration = livingAttacker.getAttribute(SBSDAttributes.MAGIC_PENETRATION.get());
                    if (magicPenetration != null) {
                        penetration = magicPenetration.getValue();
                    }
                }

                double effectiveResistance = resistance - penetration;

                if (effectiveResistance != 0.0) {
                    double multiplier = 1.0d - (effectiveResistance / 100.0d);
                    if (multiplier < 0) {
                        multiplier = 0.0;
                    }
                    float newDamage = (float) (event.getAmount() * multiplier);
                    event.setAmount(newDamage);
                }
            }
        }
    }
}
