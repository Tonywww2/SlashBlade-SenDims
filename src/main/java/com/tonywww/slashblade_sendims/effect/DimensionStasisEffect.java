package com.tonywww.slashblade_sendims.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 次元凝滞：真·次元斩施加给目标的状态。
 * <p>
 * 原版效果机制会把修饰值乘以（等级 + 1）：I 级移动与飞行速度 -80%（首领和玩家），
 * II 级为 -160%，速度被压到 0（完全定住）。II 级另外由 {@code ZenithRiftArts} 取消该目标造成的伤害和受到的击退。
 * 效果到期自动消失，不往实体上写持久化状态。
 */
public class DimensionStasisEffect extends MobEffect {
    public static final double SLOW_PER_LEVEL = -0.8D;

    private static final String SPEED_UUID = "c2a1e7f4-5b3d-4c8e-9a6f-7d1b2c3e4f51";
    private static final String FLYING_UUID = "c2a1e7f4-5b3d-4c8e-9a6f-7d1b2c3e4f52";

    public DimensionStasisEffect() {
        super(MobEffectCategory.HARMFUL, 0x2B3F8F);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_UUID, SLOW_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.FLYING_SPEED, FLYING_UUID, SLOW_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
