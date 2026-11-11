package com.tonywww.slashblade_sendims.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 增幅 (Amplify)
 * <p>
 * 每级提高 5% 攻击力，乘区为 {@link AttributeModifier.Operation#MULTIPLY_BASE}。
 * 原版效果机制会把修饰值乘以（等级 + 1），因此 III 级为 +15%，V 级为 +25%。
 */
public class AmplifyEffect extends MobEffect {

    /** 每级提供的攻击力比例（MULTIPLY_BASE） */
    public static final double ATTACK_PER_LEVEL = 0.05d;

    private static final String MODIFIER_UUID = "3d7b6a52-0c41-4f7e-9b1d-6e2a8c5f4a17";

    public AmplifyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE0582A);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, MODIFIER_UUID, ATTACK_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_BASE);
    }
}
