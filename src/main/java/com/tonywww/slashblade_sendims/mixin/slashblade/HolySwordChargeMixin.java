package com.tonywww.slashblade_sendims.mixin.slashblade;

import com.tonywww.slashblade_sendims.sa.HolySwordArts;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.capability.slashblade.SlashBladeState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Override the inherited interface default; the concrete native class has no implementation. */
@Mixin(value = SlashBladeState.class, remap = false)
public abstract class HolySwordChargeMixin implements ISlashBladeState {
    @Override public int getFullChargeTicks(LivingEntity user) {
        if (HolySwordArts.EXCALIBUR.equals(getSlashArtsKey())) return HolySwordArts.EXCALIBUR_CHARGE_TICKS;
        if (HolySwordArts.CALIBURN.equals(getSlashArtsKey())) return HolySwordArts.CALIBURN_CHARGE_TICKS;
        return SlashArts.ChargeTicks;
    }
}
