package com.tonywww.slashblade_sendims.mixin.twilightforest.finalcastle;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.world.components.structures.finalcastle.FinalCastleBossGazeboComponent;

/**
 * The roof gazebo is only a violet force field cage with "Final Castle WIP" text. Skipping it leaves an open platform
 * at the top centre of the castle, where the Twilight Blade Warden ritual is held.
 */
@Mixin(FinalCastleBossGazeboComponent.class)
public abstract class FinalCastleBossGazeboComponentMixin {
    @Inject(method = "postProcess", at = @At("HEAD"), cancellable = true)
    private void sendims$openPlatform(WorldGenLevel world, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox sbb, ChunkPos chunkPos, BlockPos blockPos, CallbackInfo ci) {
        ci.cancel();
    }
}
