package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.nbt.CompoundTag;
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
 * <p>
 * {@code #slashblade_sendims:blade_knockback_immune} 中的实体在 HIGH 优先级先行处理：清掉拔刀剑
 * 招式留下的击退系数（挑空、下压也不再生效），并取消这次由拔刀剑造成的击退。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class BladeKnockbackListener {
    private BladeKnockbackListener() {
    }

    /** 拔刀剑 KnockBackHandler 存放招式击退系数的键。 */
    private static final String BLADE_KNOCKBACK_FACTOR = "knockback_factor";

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onImmuneKnockBack(LivingKnockBackEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.getType().is(SBSDTags.EntityTypes.BLADE_KNOCKBACK_IMMUNE)) {
            return;
        }
        CompoundTag data = target.getPersistentData();
        boolean bladeFactor = data.contains(BLADE_KNOCKBACK_FACTOR);
        if (bladeFactor) {
            data.remove(BLADE_KNOCKBACK_FACTOR);
        }
        if (bladeFactor || isBladeAttack(target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingKnockBack(LivingKnockBackEvent event) {
        if (isBladeAttack(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean isBladeAttack(LivingEntity target) {
        LivingEntity attacker = target.getLastHurtByMob();
        if (attacker == null || attacker == target || target.getLastHurtByMobTimestamp() != target.tickCount) {
            return false;
        }
        return attacker.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE).isPresent();
    }
}
