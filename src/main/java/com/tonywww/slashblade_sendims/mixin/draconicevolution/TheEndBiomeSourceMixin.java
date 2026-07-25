package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicEvolutionCompat;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TheEndBiomeSource.class, priority = 2000)
public abstract class TheEndBiomeSourceMixin {
    @Shadow
    @Final
    private Holder<Biome> end;

    @Shadow
    @Final
    private Holder<Biome> highlands;

    @Shadow
    @Final
    private Holder<Biome> midlands;

    @Shadow
    @Final
    private Holder<Biome> islands;

    @Shadow
    @Final
    private Holder<Biome> barrens;

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void slashblade_sendims$preserveVanillaBiomeOutsideTarget(
            int x,
            int y,
            int z,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        TheEndBiomeSource self = (TheEndBiomeSource) (Object) this;
        if (!DraconicEvolutionCompat.isTargetEndBiomeSource(self)) {
            cir.setReturnValue(slashblade_sendims$getVanillaNoiseBiome(x, y, z, sampler));
        }
    }

    @Unique
    private Holder<Biome> slashblade_sendims$getVanillaNoiseBiome(
            int x,
            int y,
            int z,
            Climate.Sampler sampler
    ) {
        int blockX = QuartPos.toBlock(x);
        int blockY = QuartPos.toBlock(y);
        int blockZ = QuartPos.toBlock(z);
        int sectionX = SectionPos.blockToSectionCoord(blockX);
        int sectionZ = SectionPos.blockToSectionCoord(blockZ);
        if ((long) sectionX * sectionX + (long) sectionZ * sectionZ <= 4096L) {
            return this.end;
        }

        int sampleX = (sectionX * 2 + 1) * 8;
        int sampleZ = (sectionZ * 2 + 1) * 8;
        double erosion = sampler.erosion().compute(
                new DensityFunction.SinglePointContext(sampleX, blockY, sampleZ)
        );
        if (erosion > 0.25D) {
            return this.highlands;
        }
        if (erosion >= -0.0625D) {
            return this.midlands;
        }
        return erosion < -0.21875D ? this.islands : this.barrens;
    }
}