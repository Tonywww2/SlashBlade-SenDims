package com.tonywww.slashblade_sendims.mixin.cataclysm;

import com.github.L_Ender.cataclysm.structures.Burning_Arena_Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = Burning_Arena_Structure.class)
public class Burning_Arena_StructureMixin {

    @ModifyConstant(method = { "findGenerationPoint", "m_214086_" }, constant = @Constant(intValue = 21), remap = false)
    private int sendims_modifyArenaY1(int original) {
        return 93;
    }

    @ModifyConstant(method = "generatePieces", constant = @Constant(intValue = 21), remap = false)
    private static int sendims_modifyArenaY2(int original) {
        return 93;
    }
}
