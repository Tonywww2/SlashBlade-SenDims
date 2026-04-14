package com.tonywww.slashblade_sendims.mixin.cataclysm;

import com.github.L_Ender.cataclysm.structures.RuinedCitadelStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RuinedCitadelStructure.class)
public class RuinedCitadelStructureMixin {
    @Inject(method = "generatePieces", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sendims_generatePieces(StructurePiecesBuilder p_197233_, Structure.GenerationContext p_197234_, CallbackInfo ci) {
        BlockPos blockpos = new BlockPos(p_197234_.chunkPos().getMinBlockX(), 190, p_197234_.chunkPos().getMinBlockZ());
        Rotation rotation = Rotation.getRandom(p_197234_.random());
        RuinedCitadelStructure.start(p_197234_.structureTemplateManager(), blockpos, rotation, p_197233_, p_197234_.random());
        ci.cancel();
    }
}

