package com.tonywww.slashblade_sendims.mixin.umapyoi;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.registry.skills.DivineSpeedSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DivineSpeedSkill.class, remap = true)
public abstract class DivineSpeedSkillMixin extends UmaSkill {

    public DivineSpeedSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        int skillLevel = this.getSkillLevel() - 1;
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100 + (skillLevel * 60), skillLevel + 1));
        user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100 + (skillLevel * 60), skillLevel));
        ci.cancel();
    }

}
