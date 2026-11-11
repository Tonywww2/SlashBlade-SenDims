package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.SenDims;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 拔刀剑攻击不产生水平击退。
 * <p>
 * 拔刀剑自身的击退系数在 {@code KnockBackHandlerMixin} 中归零；这里补上不经过系数的击退：
 * 疾跑与击退附魔的额外击退、没有设置系数的命中等。攻击者按 hurt() 在击退前记录的
 * lastHurtByMob 判断，只处理同一 tick 内、主手持拔刀剑的攻击者造成的击退。
 * 拔刀剑在 NORMAL 优先级已经处理过的（含挑空、下压）会被取消，不会再到这里。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class BladeKnockbackListener {
    private BladeKnockbackListener() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        LivingEntity attacker = target.getLastHurtByMob();
        if (attacker == null || attacker == target || target.getLastHurtByMobTimestamp() != target.tickCount) {
            return;
        }
        if (attacker.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).isPresent()) {
            event.setCanceled(true);
        }
    }
}
