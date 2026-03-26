package com.tonywww.slashblade_sendims.mixin.umapyoi;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.registry.skills.TopUmamusumeSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TopUmamusumeSkill.class, remap = true)
public class TopUmamusumeSkillMixin extends UmaSkill {

    public TopUmamusumeSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
        user.heal(Math.min(4f, user.getMaxHealth() * 0.05f));
        UmaStatusUtils.addMotivation(user);
        ci.cancel();
    }

}