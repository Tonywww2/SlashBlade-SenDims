package com.tonywww.slashblade_sendims.mixin.umapyoi;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.utils.UmaUtils;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.registry.skills.LastLegSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LastLegSkill.class, remap = true)
public class LastLegSkillMixin extends UmaSkill {

    public LastLegSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        int skillLevel = this.getSkillLevel() - 1;
        switch (skillLevel) {
            case 0:
                user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 2));
                break;
            case 1:
                ItemStack soul = UmapyoiAPI.getUmaSoul(user);
                if (UmaSoulUtils.getProperty(soul)[2] >= 10) {
                    user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 3));
                    UmaUtils.areaSkill(level, user, (living -> {
                        living.addEffect(new MobEffectInstance(ALObjects.MobEffects.SUNDERING.get(), 80, 0));
                    }));

                } else {
                    user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 2));
                    UmaUtils.areaSkill(level, user, (living -> {
                        living.addEffect(new MobEffectInstance(ALObjects.MobEffects.SUNDERING.get(), 20, 0));
                    }));

                }
                break;
            default:
                SenDims.LOGGER.error("UMA SKILL WITH wrong level: {}{}", skillLevel, this.getClass());
                break;

        }
        ci.cancel();
    }
}
