package com.tonywww.slashblade_sendims.mixin.terracurio;

import com.tonywww.slashblade_sendims.compat.terracurio.TerraCurioArmorPass;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TerraCurio's armor wrapper reads {@code ModAttributes.getArmorPass()}; Apothic Attributes already applies
 * the mapped armor pierce, so the lookup is redirected during armor absorption only.
 */
@Pseudo
@Mixin(targets = "org.confluence.terra_curio.misc.ModAttributes", remap = false)
public abstract class ModAttributesMixin {
    @Inject(method = "getArmorPass", at = @At("RETURN"), cancellable = true, remap = false)
    private static void sendims$skipDuplicateArmorPierce(CallbackInfoReturnable<Attribute> cir) {
        Attribute replacement = TerraCurioArmorPass.replaceDuringArmorAbsorb(cir.getReturnValue());
        if (replacement != null) cir.setReturnValue(replacement);
    }
}
