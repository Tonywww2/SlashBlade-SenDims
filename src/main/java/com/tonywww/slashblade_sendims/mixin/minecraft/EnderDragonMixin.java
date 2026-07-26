package com.tonywww.slashblade_sendims.mixin.minecraft;

import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Removes the non-head damage reduction that multipart bosses apply. Vanilla's
 * {@link EnderDragon#hurt(EnderDragonPart, net.minecraft.world.damagesource.DamageSource, float)}
 * multiplies incoming damage by {@code damage / 4 + min(damage, 1)} whenever the struck part is not
 * the head. By redirecting the single {@code this.head} read used by that comparison and returning
 * the part currently being hit, the {@code part != head} check always evaluates to {@code false},
 * so the reduction branch is skipped for every part while all other combat logic is untouched.
 */
@Mixin(EnderDragon.class)
public class EnderDragonMixin {

    @Redirect(
            method = "hurt(Lnet/minecraft/world/entity/boss/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;head:Lnet/minecraft/world/entity/boss/EnderDragonPart;",
                    opcode = Opcodes.GETFIELD
            )
    )
    private EnderDragonPart sdbf$removeNonHeadDamageReduction(EnderDragon instance, EnderDragonPart part) {
        return part;
    }
}
