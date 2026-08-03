package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianEntity;
import com.brandon3055.draconicevolution.entity.guardian.DraconicGuardianPartEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Adjusts how the Draconic Guardian processes incoming attacks inside {@code attackEntityPartFrom}.
 *
 * <p>1. Non-head damage reduction: Draconic Evolution mirrors the vanilla ender dragon and applies
 * {@code damage / 4 + min(damage, 1)} whenever the struck part is not {@code dragonPartHead}.
 * Redirecting the single {@code dragonPartHead} read to the part currently being hit makes the
 * {@code part != dragonPartHead} check evaluate to {@code false}, so the reduction is skipped.
 *
 * <p>2. Per-hit damage caps: the method clamps incoming damage to {@code 500} before the shield and
 * to {@code 100} before the health pool. Replacing both constants with {@link Float#MAX_VALUE} makes
 * every {@code damage > cap} test fail, so the full damage is passed through unclamped.
 *
 * <p>The shield absorption, the crystal gate, the 5-tick hit cooldown and the phase invulnerability
 * are left untouched.
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

    @ModifyConstant(
            method = "attackEntityPartFrom",
            constant = {
                    @Constant(floatValue = 500.0F),
                    @Constant(floatValue = 100.0F)
            },
            remap = false
    )
    private float sdbf$removePerHitDamageCaps(float cap) {
        return Float.MAX_VALUE;
    }
}
