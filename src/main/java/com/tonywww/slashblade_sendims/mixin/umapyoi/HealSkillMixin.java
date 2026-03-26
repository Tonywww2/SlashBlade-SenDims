package com.tonywww.slashblade_sendims.mixin.umapyoi;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.registry.skills.HealSkill;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import net.tracen.umapyoi.utils.UmaStatusUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HealSkill.class, remap = true)
public class HealSkillMixin extends UmaSkill {

    public HealSkillMixin(Builder builder) {
        super(builder);
    }

    @Inject(method = "applySkill(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectApplySkill(Level level, LivingEntity user, CallbackInfo ci) {
        int skillLevel = this.getSkillLevel() - 1;
        ItemStack soul = UmapyoiAPI.getUmaSoul(user);
        int skillTime = UmaSoulUtils.getProperty(soul)[4] >= 10 ? 200 : (UmaSoulUtils.getProperty(soul)[4] >= 7 ? 160 : 120);

        switch (skillLevel) {
            case 0:
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, skillTime, 1));
                break;
            case 1:
                int amplifier = user.getMaxHealth() > 25000 ? 5 :
                        user.getMaxHealth() > 2500 ? 4 :
                        user.getMaxHealth() > 250 ? 3 :
                        user.getMaxHealth() > 160 ? 2 :
                        user.getMaxHealth() > 40 ? 1 : 0;
                user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, skillTime, amplifier));
                break;
            default:
                SenDims.LOGGER.error("UMA SKILL WITH wrong level: {}{}", skillLevel, this.getClass());
                break;
        }
        ci.cancel();
    }
}
