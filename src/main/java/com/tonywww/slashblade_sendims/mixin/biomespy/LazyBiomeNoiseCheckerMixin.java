package com.tonywww.slashblade_sendims.mixin.biomespy;

import com.tonywww.slashblade_sendims.compat.biomespy.ClimateCoverage;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * BiomeSpy replaces the structure and biome search in multi_noise dimensions. Before checking a
 * candidate chunk it rejects climates outside the combined ranges of the target biomes, which only
 * holds when the ranges tile the climate space. In dimensions with gaps the nearest entry wins, so
 * e.g. Chulak's stargate chunk was always rejected and dialing Chulak failed until someone loaded
 * that chunk. There, accept every climate axis and let the vanilla check decide. BiomeSpy's
 * "none of these biomes exist in this dimension" shortcut runs before this method and is kept.
 */
@Pseudo
@Mixin(targets = "com.moepus.biomespy.biome.LazyBiomeNoiseChecker", remap = false)
public abstract class LazyBiomeNoiseCheckerMixin {
    @Shadow(remap = false)
    @Final
    private Climate.ParameterList<Holder<Biome>> parameters;

    @Inject(method = "checkSingleNoise", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void sendims$acceptGappedClimate(CallbackInfoReturnable<Boolean> cir) {
        if (!ClimateCoverage.coversClimateSpace(this.parameters)) {
            cir.setReturnValue(true);
        }
    }
}
