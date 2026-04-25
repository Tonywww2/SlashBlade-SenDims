package com.tonywww.slashblade_sendims.utils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.effect.ItemEffect;
import se.mickelus.tetra.items.modular.IModularItem;

public class TetraUtils {

    public static int getEffectLvl(ItemStack stack, IModularItem modularItem, ItemEffect effect) {
        return modularItem.getEffectData(stack).getLevel(effect);
    }

    /**
     * 获取生物主手物品和4件护甲的指定improvement总等级
     * @param entity 生物实体
     * @param effect ItemEffect
     * @return 总登记等级
     */
    public static int getEffectLvlTotal(LivingEntity entity, ItemEffect effect) {
        int totalLvl = 0;

        // 检查主手物品
        ItemStack mainHandItem = entity.getMainHandItem();
        if (!mainHandItem.isEmpty() && mainHandItem.getItem() instanceof IModularItem modularItem) {
            totalLvl += getEffectLvl(mainHandItem, modularItem, effect);
        }

        // 检查4件护甲（头、胸、腿、脚）
        for (ItemStack armorStack : entity.getArmorSlots()) {
            if (!armorStack.isEmpty() && armorStack.getItem() instanceof IModularItem modularItem) {
                totalLvl += getEffectLvl(armorStack, modularItem, effect);
            }
        }

        return totalLvl;
    }
}
