package com.tonywww.slashblade_sendims.mixin.apothicattributes;

import dev.shadowsoffire.attributeslib.impl.AttributeEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
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

@Mixin(value = AttributeEvents.class, remap = false)
public class AttributeEventsMixin {

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
            target.invulnerableTime = 0;
            if (fireDmg > 0.001 && AttributesLib.localAtkStrength >= 0.55F) {
                target.hurt(src(ALObjects.DamageTypes.FIRE_DAMAGE, attacker), AttributesLib.localAtkStrength * fireDmg);
                target.setRemainingFireTicks(Math.min(Math.max(400, target.getRemainingFireTicks()), target.getRemainingFireTicks() + (int) (4 * fireDmg)));
            }
            target.invulnerableTime = 0;
            if (coldDmg > 0.001 && AttributesLib.localAtkStrength >= 0.55F) {
                target.hurt(src(ALObjects.DamageTypes.COLD_DAMAGE, attacker), AttributesLib.localAtkStrength * coldDmg);
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
}
