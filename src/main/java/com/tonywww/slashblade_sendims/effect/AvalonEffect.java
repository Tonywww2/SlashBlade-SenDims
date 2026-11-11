package com.tonywww.slashblade_sendims.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 阿瓦隆：只用于显示。真正的无敌由拔刀剑闪避使用的
 * {@code mods.flammpfeil.slashblade.ability.Untouchable#setUntouchable} 提供，两者时长一致。
 */
public class AvalonEffect extends MobEffect {
    public AvalonEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF2CC55);
    }
}
