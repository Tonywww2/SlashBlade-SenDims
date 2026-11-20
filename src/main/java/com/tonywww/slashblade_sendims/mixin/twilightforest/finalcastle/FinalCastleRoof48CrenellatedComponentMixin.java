package com.tonywww.slashblade_sendims.mixin.twilightforest.finalcastle;

import com.tonywww.slashblade_sendims.twilight.FinalCastleDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.world.components.structures.finalcastle.FinalCastleRoof48CrenellatedComponent;

/** Violet key chest, its guard and the parapet force field. */
@Mixin(FinalCastleRoof48CrenellatedComponent.class)
public abstract class FinalCastleRoof48CrenellatedComponentMixin {
    @Inject(method = "postProcess", at = @At("TAIL"))
    private void sendims$decorate(WorldGenLevel world, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox sbb, ChunkPos chunkPos, BlockPos blockPos, CallbackInfo ci) {
        FinalCastleDecorator.decorateRoof((StructurePiece) (Object) this, world, sbb);
    }
}
