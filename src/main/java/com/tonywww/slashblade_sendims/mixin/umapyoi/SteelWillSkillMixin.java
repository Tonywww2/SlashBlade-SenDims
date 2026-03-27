package com.tonywww.slashblade_sendims.mixin.umapyoi;

import com.tonywww.slashblade_sendims.SenDims;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.registry.skills.SteelWillSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SteelWillSkill.class, remap = true)
public class SteelWillSkillMixin extends UmaSkill {

    public SteelWillSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        int skillLevel = this.getSkillLevel() - 1;
        switch (skillLevel) {
            case 0:
                user.addEffect(new MobEffectInstance(ALObjects.MobEffects.KNOWLEDGE.get(), 200, 0));
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                break;
            case 1:
                user.addEffect(new MobEffectInstance(ALObjects.MobEffects.KNOWLEDGE.get(), 200, 0));
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
                user.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
                UmaStatusUtils.addMotivation(user);
                break;
            default:
                SenDims.LOGGER.error("UMA SKILL WITH wrong level: {}{}", skillLevel, this.getClass());
                break;

        }
        ci.cancel();
    }
}
