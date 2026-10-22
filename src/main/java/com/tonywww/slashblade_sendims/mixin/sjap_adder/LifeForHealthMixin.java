package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.se.kamuy.LifeForHealth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LifeForHealth.class, remap = false)
public class LifeForHealthMixin {

    @Redirect(method = "handleLivingHurtAttacker", at = @At(value = "INVOKE", target = "Lcom/dinzeer/sjapadder/se/kamuy/LifeForHealth;calculateReducedDamage(F)F"))
    private float onCalculateReducedDamage(float damage, LivingHurtEvent event, LivingEntity holder) {
        if (damage <= 0.0F || damage < holder.getMaxHealth() * 0.2F) return damage;
        float reduction = 0.05F + holder.getRandom().nextFloat() * 0.1F;
        return damage * (1.0F - reduction);
    }
}
