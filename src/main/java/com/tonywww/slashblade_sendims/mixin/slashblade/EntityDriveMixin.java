package com.tonywww.slashblade_sendims.mixin.slashblade;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.util.KnockBacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityDrive.class)
public class EntityDriveMixin {
    @Redirect(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lmods/flammpfeil/slashblade/util/KnockBacks;ordinal()I"))
    private int onGetKnockbackOrdinal(KnockBacks instance) {
        if (instance == null) {
            return 0;
        }
        return instance.ordinal();
    }
}

