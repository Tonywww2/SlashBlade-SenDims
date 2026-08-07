package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicEvolutionCompat;
import com.tonywww.slashblade_sendims.compat.draconicevolution.EndIslandNoiseAccessor;
import com.tonywww.slashblade_sendims.compat.draconicevolution.EndIslandWorldgenCompat;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction",
    priority = 500
)
public abstract class EndIslandDensityFunctionMixin implements EndIslandNoiseAccessor {
    @Shadow
    @Final
    private SimplexNoise islandNoise;

    @Override
    @Unique
    public SimplexNoise slashblade_sendims$getIslandNoise() {
        return this.islandNoise;
    }

    @Inject(
            method = "getHeightValue(Lnet/minecraft/world/level/levelgen/synth/SimplexNoise;II)F",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void slashblade_sendims$preserveVanillaHeightOutsideTarget(
            SimplexNoise noise,
            int x,
            int z,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!DraconicEvolutionCompat.isTargetEndIslandNoise(noise)) {
            cir.setReturnValue(EndIslandWorldgenCompat.getVanillaHeightValue(noise, x, z));
        }
    }
}