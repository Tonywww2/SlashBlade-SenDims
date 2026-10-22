package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.se.nihil.WaveEdgeRiseUp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = WaveEdgeRiseUp.class, remap = false)
public class WaveEdgeRiseUpMixin {

    @ModifyConstant(method = "handleDoSlash", constant = @Constant(doubleValue = 0.7D))
    private double onGetChanceThreshold(double threshold) {
        return 0.8D;
    }

    @ModifyArg(method = "handleDoSlash", at = @At(value = "INVOKE", target = "Lmods/flammpfeil/slashblade/slasharts/WaveEdge;doSlash(Lnet/minecraft/world/entity/LivingEntity;FILnet/minecraft/world/phys/Vec3;ZDFFI)V"), index = 5)
    private double onGetWaveDamage(double damage) {
        return 0.8D;
    }
}
