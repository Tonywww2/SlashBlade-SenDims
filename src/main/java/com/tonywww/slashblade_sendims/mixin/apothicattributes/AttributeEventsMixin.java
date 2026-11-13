package com.tonywww.slashblade_sendims.mixin.apothicattributes;

import dev.shadowsoffire.attributeslib.impl.AttributeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import dev.shadowsoffire.attributeslib.AttributesLib;
import dev.shadowsoffire.attributeslib.util.AttributesUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AttributeEvents.class, remap = false)
public class AttributeEventsMixin {

    /** 攻击冷却为 0 时元素伤害的倍率。 */
    @Unique
    private static final float SENDIMS$MIN_ELEMENTAL_SCALE = 0.2F;
    /** 攻击冷却达到该比例时元素伤害为满额。 */
    @Unique
    private static final float SENDIMS$FULL_ELEMENTAL_COOLDOWN = 0.6F;

    @Shadow
    private static boolean noRecurse;

    @Shadow
    private static DamageSource src(ResourceKey<DamageType> type, LivingEntity entity) {
        return null;
    }

    @Inject(method = "meleeDamageAttributes", at = @At("HEAD"), cancellable = true)
    private void takeOverMeleeDamageAttributes(LivingAttackEvent e, CallbackInfo ci) {
        if (e.getEntity().level().isClientSide || e.getEntity().isDeadOrDying()) {
            ci.cancel();
            return;
        }
        if (noRecurse) {
            ci.cancel();
            return;
        }
        noRecurse = true;
        if (e.getSource().getDirectEntity() instanceof LivingEntity attacker && AttributesUtil.isPhysicalDamage(e.getSource())) {
            float hpDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.CURRENT_HP_DAMAGE.get());
            float fireDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.FIRE_DAMAGE.get());
            float coldDmg = (float) attacker.getAttributeValue(ALObjects.Attributes.COLD_DAMAGE.get());
            LivingEntity target = e.getEntity();
            int time = target.invulnerableTime;
            target.invulnerableTime = 0;
            if (hpDmg > 0.001 && AttributesLib.localAtkStrength >= 0.85F) {
                target.hurt(src(ALObjects.DamageTypes.CURRENT_HP_DAMAGE, attacker), AttributesLib.localAtkStrength * hpDmg * target.getHealth());
            }
            // 元素伤害不设攻击冷却门槛，按冷却缩放：冷却 0% 时 20%，60% 及以上 100%，中间线性。
            float elementScale = sendims$elementalCooldownScale(AttributesLib.localAtkStrength);
            target.invulnerableTime = 0;
            if (fireDmg > 0.001) {
                target.hurt(src(ALObjects.DamageTypes.FIRE_DAMAGE, attacker), elementScale * fireDmg);
                target.setRemainingFireTicks(Math.min(Math.max(400, target.getRemainingFireTicks()), target.getRemainingFireTicks() + (int) (4 * fireDmg)));
            }
            target.invulnerableTime = 0;
            if (coldDmg > 0.001) {
                target.hurt(src(ALObjects.DamageTypes.COLD_DAMAGE, attacker), elementScale * coldDmg);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.min(100, (int) (2 * coldDmg)), Math.min(3, Mth.floor(coldDmg / 25))));
            }
            target.invulnerableTime = time;
            if (target.isDeadOrDying()) {
                target.getPersistentData().putBoolean("apoth.killed_by_aux_dmg", true);
            }
        }
        noRecurse = false;
        ci.cancel();
    }

    @Unique
    private static float sendims$elementalCooldownScale(float attackStrength) {
        float progress = Mth.clamp(attackStrength / SENDIMS$FULL_ELEMENTAL_COOLDOWN, 0.0F, 1.0F);
        return SENDIMS$MIN_ELEMENTAL_SCALE + (1.0F - SENDIMS$MIN_ELEMENTAL_SCALE) * progress;
    }

    @Inject(method = "lifeStealOverheal", at = @At("HEAD"), cancellable = true)
    private void takeOverLifeStealOverheal(LivingHurtEvent e, CallbackInfo ci) {
        ci.cancel();
    }

    @Unique
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void sendims$onLivingDeathLifesteal(LivingDeathEvent e) {
        if (e.getSource().getDirectEntity() instanceof LivingEntity attacker && AttributesUtil.isPhysicalDamage(e.getSource())) {
            float lifesteal = (float) attacker.getAttributeValue(ALObjects.Attributes.LIFE_STEAL.get());
            float amount = e.getEntity().getMaxHealth() * lifesteal;
            if (amount > 0) {
                attacker.heal(Math.min(1, amount));
            }
        }
    }
}
