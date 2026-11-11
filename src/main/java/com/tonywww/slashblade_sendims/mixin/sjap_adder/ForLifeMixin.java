package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.legendreliclib.lib.util.FastMakeEntityUtil;
import com.dinzeer.sjapadder.sa.ForLife;
import com.tonywww.slashblade_sendims.compat.sjap.LifeSeries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 莉芙系列 SA「颂生者」「EX·段生铸念」：去掉抗性提升，并在效果施加后补上增幅与额外一刀。
 */
@Mixin(value = ForLife.class, remap = false)
public abstract class ForLifeMixin {

    @Redirect(method = {"doForLife", "doForLife2"}, at = @At(value = "INVOKE",
            target = "Lcom/dinzeer/legendreliclib/lib/util/FastMakeEntityUtil;FastEffect(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffect;I)V"),
            require = 1)
    private static void sendims$skipResistance(LivingEntity entity, MobEffect effect, int level) {
        if (effect == MobEffects.DAMAGE_RESISTANCE) {
            return;
        }
        FastMakeEntityUtil.FastEffect(entity, effect, level);
    }

    @Inject(method = "doForLife", at = @At("TAIL"), require = 1)
    private static void sendims$afterForLife(LivingEntity entity, CallbackInfo ci) {
        LifeSeries.afterForLife(entity, false);
    }

    @Inject(method = "doForLife2", at = @At("TAIL"), require = 1)
    private static void sendims$afterForLifeDrive(LivingEntity entity, CallbackInfo ci) {
        LifeSeries.afterForLife(entity, true);
    }
}
