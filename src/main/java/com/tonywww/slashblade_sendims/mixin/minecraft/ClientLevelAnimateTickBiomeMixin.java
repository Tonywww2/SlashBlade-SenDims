package com.tonywww.slashblade_sendims.mixin.minecraft;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * animateTick samples about 1300 random blocks per client tick and asks for each one's biome only to pick its
 * ambient particle. BiomeManager.getBiome runs the fuzzed eight-corner lookup every time, which showed up as
 * the largest single cost of the client tick. Read the 4x4x4 noise biome cell directly instead; ambient
 * particles may shift by a couple of blocks at biome borders.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelAnimateTickBiomeMixin {
    @Redirect(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
    private Holder<Biome> sdbf$noiseBiomeForAmbientParticles(ClientLevel level, BlockPos pos) {
        return level.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
    }
}
