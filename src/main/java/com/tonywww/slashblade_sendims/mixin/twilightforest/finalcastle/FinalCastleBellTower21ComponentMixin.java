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
import twilightforest.world.components.structures.finalcastle.FinalCastleBellTower21Component;

/** Blue key chest in place of the "Parkour area 2" sign, and its guard. */
@Mixin(FinalCastleBellTower21Component.class)
public abstract class FinalCastleBellTower21ComponentMixin {
    @Inject(method = "postProcess", at = @At("TAIL"))
    private void sendims$decorate(WorldGenLevel world, StructureManager manager, ChunkGenerator generator, RandomSource random, BoundingBox sbb, ChunkPos chunkPos, BlockPos blockPos, CallbackInfo ci) {
        FinalCastleDecorator.decorateBellTower((StructurePiece) (Object) this, world, sbb);
    }
}
