package com.tonywww.slashblade_sendims.mixin.umapyoi;

import com.tonywww.slashblade_sendims.SenDims;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.effect.MobEffectRegistry;
import net.tracen.umapyoi.registry.skills.LowHealthHealSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LowHealthHealSkill.class, remap = true)
public class LowHealthHealSkillMixin extends UmaSkill {

    public LowHealthHealSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        int skillLevel = this.getSkillLevel() - 1;
        if (user.hasEffect(MobEffectRegistry.PANICKING.get())) {
            user.removeEffect(MobEffectRegistry.PANICKING.get());
        }
        switch (skillLevel) {
            case 0:
                user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                user.addEffect(new MobEffectInstance(ALObjects.MobEffects.VITALITY.get(), 200, 0));
                break;
            case 1:
                user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2));
                user.addEffect(new MobEffectInstance(ALObjects.MobEffects.VITALITY.get(), 200, 0));
                break;
            default:
                SenDims.LOGGER.error("UMA SKILL WITH wrong level: {}{}", skillLevel, this.getClass());
                break;
        }
        ci.cancel();
    }
}
