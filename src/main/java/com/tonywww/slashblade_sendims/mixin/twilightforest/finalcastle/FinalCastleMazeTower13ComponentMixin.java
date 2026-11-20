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
import twilightforest.world.components.structures.finalcastle.FinalCastleMazeTower13Component;

/** Maze tower chests and the yellow key in the entrance tower (subclasses without their own postProcess end here too). */
@Mixin(FinalCastleMazeTower13Component.class)
public abstract class FinalCastleMazeTower13ComponentMixin {
    @Inject(method = "postProcess", at = @At("TAIL"))
    private void sendims$decorate(WorldGenLevel world, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox sbb, ChunkPos chunkPos, BlockPos blockPos, CallbackInfo ci) {
        FinalCastleDecorator.decorateMazeTower((StructurePiece) (Object) this, world, sbb);
    }
}
