package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.effect.ForLifes;
import com.dinzeer.sjapadder.register.SjaEffect;
import com.tonywww.slashblade_sendims.compat.sjap.LifeSeries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 生露：原版的"取消伤害并在结束时按 155% 回结算"整体停用，
 * 新逻辑在 {@link com.tonywww.slashblade_sendims.compat.sjap.LifeSeries} 中实现。
 * 原版同一监听器里"生露期间造成的伤害 ×(1 + 0.3×等级)"的增伤保留并下调为每级 10%，且不作用于生露结束时的回敬伤害。
 */
@Mixin(value = ForLifes.class, remap = false)
public abstract class ForLifesMixin {
    /** 原版每级 30%；SA 本身已提供增幅，下调为每级 10%。 */
    @Unique
    private static final float DAMAGE_BONUS_PER_LEVEL = 0.1F;

    @Inject(method = "recordDamage", at = @At("HEAD"), cancellable = true, require = 1)
    private static void sendims$keepBoostOnly(LivingHurtEvent event, CallbackInfo ci) {
        ci.cancel();
        if (LifeSeries.isSettling()) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            MobEffectInstance dew = attacker.getEffect(SjaEffect.ForLife.get());
            if (dew != null) {
                event.setAmount(event.getAmount() * (dew.getAmplifier() * DAMAGE_BONUS_PER_LEVEL + 1.0F));
            }
        }
    }

    @Inject(method = "removeAttributeModifiers", at = @At("HEAD"), cancellable = true, require = 1, remap = true)
    private void sendims$disableSettlement(LivingEntity entity, AttributeMap map, int amplifier, CallbackInfo ci) {
        ci.cancel();
    }
}
