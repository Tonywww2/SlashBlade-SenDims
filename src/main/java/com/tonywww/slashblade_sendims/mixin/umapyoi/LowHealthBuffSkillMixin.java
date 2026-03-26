package com.tonywww.slashblade_sendims.mixin.umapyoi;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.utils.UmaUtils;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.effect.MobEffectRegistry;
import net.tracen.umapyoi.registry.skills.LowHealthBuffSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LowHealthBuffSkill.class, remap = true)
public class LowHealthBuffSkillMixin extends UmaSkill {

    public LowHealthBuffSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        int skillLevel = this.getSkillLevel() - 1;
        if (user.hasEffect(MobEffectRegistry.PANICKING.get())) {
            user.removeEffect(MobEffectRegistry.PANICKING.get());
        }
        UmaStatusUtils.addMotivation(user);
        switch (skillLevel) {
            case 0:
                UmaUtils.areaSkill(level, user, (living -> {
                    living.addEffect(new MobEffectInstance(ALObjects.MobEffects.GRIEVOUS.get(), 200, 0));
                }));
                break;
            case 1:
                boolean lowHealth = (double)(user.getHealth() / user.getMaxHealth()) < 0.6;
                UmaStatusUtils.addMotivation(user);

                UmaUtils.areaSkill(level, user, (living -> {
                    living.addEffect(new MobEffectInstance(ALObjects.MobEffects.GRIEVOUS.get(), lowHealth ? 400 : 300, lowHealth ? 2 : 1));
                }));

                break;
            default:
                SenDims.LOGGER.error("UMA SKILL WITH wrong level: {}{}", skillLevel, this.getClass());

        }
        ci.cancel();
    }

}
