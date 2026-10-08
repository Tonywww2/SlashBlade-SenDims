package com.tonywww.slashblade_sendims.effect;

import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

/**
 * 风暴穿透 (Storm Penetration)
 * <p>
 * 无名王者系列 SE「风暴」在释放 SA 时赋予自身的增益。
 * 每一级提供 5 点魔法穿透（{@link SBSDAttributes#MAGIC_PENETRATION}）。
 * <p>
 * 由于魔法穿透数值需要随效果等级线性增长，这里使用
 * {@link #addAttributeModifiers}/{@link #removeAttributeModifiers} 手动挂载修饰符，
 * 而不是依赖固定的属性修饰符列表。
 */
public class StormPenetrationEffect extends MobEffect {

    /** 每级提供的魔法穿透数值 */
    public static final double PENETRATION_PER_LEVEL = 5.0d;

    private static final UUID MODIFIER_UUID = UUID.fromString("9f2c1a44-7b3e-4d21-9a86-5d0f1e7b3c10");
    private static final String MODIFIER_NAME = "effect.slashblade_sendims.storm_penetration";

    public StormPenetrationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4FC3FF);
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        AttributeInstance instance = attributeMap.getInstance(SBSDAttributes.MAGIC_PENETRATION.get());
        if (instance == null) {
            return;
        }

        // 等级变化时会重复调用，先移除旧修饰符保证幂等
        instance.removeModifier(MODIFIER_UUID);
        instance.addTransientModifier(new AttributeModifier(
                MODIFIER_UUID,
                MODIFIER_NAME,
                PENETRATION_PER_LEVEL * (amplifier + 1),
                AttributeModifier.Operation.ADDITION
        ));
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        AttributeInstance instance = attributeMap.getInstance(SBSDAttributes.MAGIC_PENETRATION.get());
        if (instance != null) {
            instance.removeModifier(MODIFIER_UUID);
        }
    }
}
