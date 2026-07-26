package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity;
import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianPartEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Removes the Draconic Guardian's non-head damage reduction. Draconic Evolution mirrors the vanilla
 * ender dragon: inside {@code attackEntityPartFrom} it applies {@code damage / 4 + min(damage, 1)}
 * whenever the struck part is not {@code dragonPartHead}. Redirecting the single {@code dragonPartHead}
 * read to the part currently being hit makes the {@code part != dragonPartHead} check evaluate to
 * {@code false}, so the reduction is skipped for every part. The shield absorption, the 500/100
 * per-hit caps, the crystal gate and the phase invulnerability all remain in effect.
 */
@Mixin(DraconicGuardianEntity.class)
public class DraconicGuardianEntityMixin {

    @Redirect(
            method = "attackEntityPartFrom",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/brandon3055/draconicevolution/entity/guardian/DraconicGuardianEntity;dragonPartHead:Lcom/brandon3055/draconicevolution/entity/guardian/DraconicGuardianPartEntity;",
                    opcode = Opcodes.GETFIELD
            ),
            remap = false
    )
    private DraconicGuardianPartEntity sdbf$removeNonHeadDamageReduction(DraconicGuardianEntity instance, DraconicGuardianPartEntity part) {
        return part;
    }
}
