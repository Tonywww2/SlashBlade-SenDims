package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.event.handler.KnockBackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 拔刀剑斩击不再产生水平击退：所有 {@code KnockBacks} 的水平系数归零。
 * 原本只有 {@code smash} 带水平系数（1.5），归零后与 {@code cancel} 一样取消水平击退；
 * {@code toss}、{@code meteor} 的纵向挑空、下压不受影响。
 */
@Mixin(value = KnockBackHandler.class, remap = false)
public abstract class KnockBackHandlerMixin {

    @ModifyVariable(method = "setFactor(Lnet/minecraft/world/entity/LivingEntity;DDD)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false)
    private static double sendims$noHorizontalKnockback(double horizontalFactor) {
        return 0.0D;
    }
}
