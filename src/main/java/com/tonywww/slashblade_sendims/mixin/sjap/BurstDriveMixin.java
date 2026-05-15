package com.tonywww.slashblade_sendims.mixin.sjap;

import cn.mmf.slashblade_addon.specialeffect.BurstDrive;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BurstDrive.class, remap = false)
public class BurstDriveMixin {

    @Redirect(method = "onDoingSlash", at = @At(value = "INVOKE", target = "Lmods/flammpfeil/slashblade/event/SlashBladeEvent$DoSlashEvent;getDamage()D"))
    private static double onGetDamage(SlashBladeEvent.DoSlashEvent instance) {
        return instance.getDamage() * 0.1D;
    }
}

