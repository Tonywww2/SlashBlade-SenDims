package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.util.KnockBacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityDrive.class)
public class EntityDriveMixin {

    @Redirect(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lmods/flammpfeil/slashblade/entity/EntityDrive;getKnockBack()Lmods/flammpfeil/slashblade/util/KnockBacks;", remap = false))
    private KnockBacks onGetKnockback(EntityDrive instance) {
        KnockBacks kb = instance.getKnockBack();
        if (kb == null) {
            return KnockBacks.cancel;
        }
        return kb;
    }
}
