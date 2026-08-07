package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.draconicevolution.DEConfig;
import com.brandon3055.draconicevolution.world.ChaosIslandFeature;
import com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicEvolutionCompat;
import com.tonywww.slashblade_sendims.worldgen.RemappedEndBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RemappedEndBiomeSource.class, priority = 2000)
public abstract class RemappedEndBiomeSourceMixin {
    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void slashblade_sendims$applyChaosIslandBiome(
            int quartX,
            int quartY,
            int quartZ,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir
    ) {
        BiomeSource self = (BiomeSource) (Object) this;
        if (!DEConfig.chaosIslandEnabled || !DraconicEvolutionCompat.isTargetEndBiomeSource(self)) {
            return;
        }

        int blockX = QuartPos.toBlock(quartX);
        int blockZ = QuartPos.toBlock(quartZ);
        ChunkPos currentChunk = new ChunkPos(blockX / 16, blockZ / 16);
        ChunkPos closestSpawn = ChaosIslandFeature.getClosestSpawn(currentChunk);
        if ((closestSpawn.x != 0 || closestSpawn.z != 0)
                && ChaosIslandFeature.overrideBiome(currentChunk, closestSpawn)) {
            cir.setReturnValue(((RemappedEndBiomeSource) (Object) this).getEndBiome());
        }
    }
}