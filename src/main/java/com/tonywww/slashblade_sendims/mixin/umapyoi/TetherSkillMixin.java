package com.tonywww.slashblade_sendims.mixin.umapyoi;

import com.tonywww.slashblade_sendims.utils.UmaUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.registry.skills.TetherSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TetherSkill.class, remap = true)
public class TetherSkillMixin extends UmaSkill {

    public TetherSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        int skillLevel = this.getSkillLevel() - 1;
        UmaUtils.areaSkill(level, user, (living -> {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3));
        }));
        if (skillLevel >= 1) {
            UmaUtils.areaSkill(level, user, (living -> {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3));
            }));
        }
        ci.cancel();
    }
}
