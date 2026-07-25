package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.draconicevolution.world.ChaosIslandFeature;
import com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicEvolutionCompat;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChaosIslandFeature.class, priority = 2000)
public abstract class ChaosIslandFeatureMixin {
    @Inject(
            method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private void slashblade_sendims$restrictChaosIsland(
            FeaturePlaceContext<NoneFeatureConfiguration> context,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!DraconicEvolutionCompat.isTargetDimension(context.level())) {
            cir.setReturnValue(false);
        }
    }
}