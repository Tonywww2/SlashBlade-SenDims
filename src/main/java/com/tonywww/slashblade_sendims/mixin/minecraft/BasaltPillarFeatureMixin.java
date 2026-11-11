package com.tonywww.slashblade_sendims.mixin.minecraft;

import com.tonywww.slashblade_sendims.worldgen.DeepRealmPillarBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.BasaltPillarFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 起点在深境1层深岩洞窟的玄武岩柱直接放置方解石（见 DeepRealmPillarBlocks）。 */
@Mixin(BasaltPillarFeature.class)
public abstract class BasaltPillarFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"))
    private void sdbf$beginPillar(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        DeepRealmPillarBlocks.beginBasaltPillar(context.level(), context.origin());
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void sdbf$endPillar(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        DeepRealmPillarBlocks.endBasaltPillar();
    }

    @Redirect(method = {"place", "placeBaseHangOff", "placeHangOff"}, at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/Blocks;BASALT:Lnet/minecraft/world/level/block/Block;",
            opcode = Opcodes.GETSTATIC), require = 4)
    private Block sdbf$pillarBlock() {
        return DeepRealmPillarBlocks.basaltPillarBlock();
    }
}
