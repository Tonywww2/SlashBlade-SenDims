package com.tonywww.slashblade_sendims.mixin.minecraft;

import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Piece-local to world coordinates, honouring the piece's orientation (used by the Final Castle decorations). */
@Mixin(StructurePiece.class)
public interface StructurePieceWorldPosInvoker {
    @Invoker("getWorldX")
    int sendims$getWorldX(int x, int z);

    @Invoker("getWorldY")
    int sendims$getWorldY(int y);

    @Invoker("getWorldZ")
    int sendims$getWorldZ(int x, int z);
}
