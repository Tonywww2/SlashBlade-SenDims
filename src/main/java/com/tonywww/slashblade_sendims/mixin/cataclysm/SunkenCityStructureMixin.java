package com.tonywww.slashblade_sendims.mixin.cataclysm;

import com.github.L_Ender.cataclysm.structures.Sunken_City_Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Sunken_City_Structure.class)
public class SunkenCityStructureMixin {

    @ModifyArg(
        method = "generatePieces",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;<init>(III)V"),
        index = 1
    )
    private static int modifyGenerationHeight(int originalY) {
        return 40;
    }
}

