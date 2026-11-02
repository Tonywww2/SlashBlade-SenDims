package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 按攻击者的 {@link SBSDAttributes#KNOCKBACK_DEALT} 缩放其造成的击退。
 * LivingKnockBackEvent 不携带攻击者；受击时 hurt() 会在击退前记录 lastHurtByMob，
 * 因此只处理同一 tick 内由该攻击者造成的击退（含命中击退、额外击退与拔刀剑招式击退）。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class KnockbackDealtListener {
    private KnockbackDealtListener() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        LivingEntity attacker = target.getLastHurtByMob();
        if (attacker == null || attacker == target || target.getLastHurtByMobTimestamp() != target.tickCount) {
            return;
        }
        AttributeInstance instance = attacker.getAttribute(SBSDAttributes.KNOCKBACK_DEALT.get());
        if (instance == null) {
            return;
        }
        double scale = instance.getValue();
        if (scale != 1.0d) {
            event.setStrength((float) (event.getStrength() * Math.max(0.0d, scale)));
        }
    }
}
