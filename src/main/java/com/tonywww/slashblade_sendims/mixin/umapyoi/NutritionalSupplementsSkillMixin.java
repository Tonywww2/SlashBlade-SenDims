package com.tonywww.slashblade_sendims.mixin.umapyoi;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.registry.skills.NutritionalSupplementsSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NutritionalSupplementsSkill.class, remap = true)
public class NutritionalSupplementsSkillMixin extends UmaSkill {

    public NutritionalSupplementsSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        int skillLevel = this.getSkillLevel() - 1;
        switch (skillLevel) {
            case 0:
                user.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0));
                break;
            case 1:
                user.addEffect(new MobEffectInstance(MobEffects.SATURATION, 80, 1));
                break;
            default:
                SenDims.LOGGER.error("UMA SKILL WITH wrong level: {}{}", skillLevel, this.getClass());
                break;
        }
        ci.cancel();
    }
}
