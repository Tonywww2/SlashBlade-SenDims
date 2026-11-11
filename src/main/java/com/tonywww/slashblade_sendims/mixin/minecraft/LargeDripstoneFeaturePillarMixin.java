package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.worldgen.DeepRealmPillarBlocks;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LargeDripstoneFeature;
import net.minecraft.world.level.levelgen.feature.configurations.LargeDripstoneConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 记录本次大型滴水石所在维度，供 LargeDripstonePillarBlockMixin 选择方块。 */
@Mixin(LargeDripstoneFeature.class)
public abstract class LargeDripstoneFeaturePillarMixin {
    @Inject(method = "place", at = @At("HEAD"))
    private void sdbf$beginPillar(FeaturePlaceContext<LargeDripstoneConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        DeepRealmPillarBlocks.beginLargeDripstone(context.level());
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void sdbf$endPillar(FeaturePlaceContext<LargeDripstoneConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        DeepRealmPillarBlocks.endLargeDripstone();
    }
}
