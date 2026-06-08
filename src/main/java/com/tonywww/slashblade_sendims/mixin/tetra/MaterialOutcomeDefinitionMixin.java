package com.tonywww.slashblade_sendims.mixin.tetra;

import com.tonywww.slashblade_sendims.utils.IMaterialData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import se.mickelus.tetra.module.data.MaterialData;
import se.mickelus.tetra.module.schematic.MaterialOutcomeDefinition;
import se.mickelus.tetra.module.schematic.OutcomeDefinition;

@Mixin(MaterialOutcomeDefinition.class)
public class MaterialOutcomeDefinitionMixin {
    @Inject(method = "combine", at = @At("RETURN"), remap = false)
    private void injectMaterialFactor(MaterialData materialData, CallbackInfoReturnable<OutcomeDefinition> cir) {
        cir.getReturnValue().material.count = (int) (cir.getReturnValue().material.count * IMaterialData.cast(materialData).slashBlade_SenDims$getCountFactor());
    }
}
