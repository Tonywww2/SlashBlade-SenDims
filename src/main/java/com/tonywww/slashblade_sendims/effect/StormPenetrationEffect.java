package com.tonywww.slashblade_sendims.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 风暴穿透 (Storm Penetration)
 * <p>
 * 无名王者系列 SE「风暴」在释放 SA 时赋予自身的增益。
 * 每一级提高 5% 攻击力，乘区为 {@link AttributeModifier.Operation#MULTIPLY_TOTAL}。
 * 原版效果机制会把修饰值乘以（等级 + 1），因此这里只登记单级数值。
 */
public class StormPenetrationEffect extends MobEffect {

    /** 每级提供的攻击力比例（MULTIPLY_TOTAL） */
    public static final double ATTACK_PER_LEVEL = 0.05d;

    private static final String MODIFIER_UUID = "9f2c1a44-7b3e-4d21-9a86-5d0f1e7b3c10";

    public StormPenetrationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4FC3FF);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, MODIFIER_UUID, ATTACK_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
