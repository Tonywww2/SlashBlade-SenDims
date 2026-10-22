package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import cn.mmf.slashblade_addon.specialattacks.WaterDrive;
import com.dinzeer.sjapadder.se.kamuy.Terrifyingwaves;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Terrifyingwaves.class, remap = false)
public class TerrifyingwavesMixin {

    @Redirect(method = "handleDoSlash", at = @At(value = "INVOKE", target = "Lcn/mmf/slashblade_addon/specialattacks/WaterDrive;doSlash(Lnet/minecraft/world/entity/LivingEntity;FILnet/minecraft/world/phys/Vec3;ZDF)Lmods/flammpfeil/slashblade/entity/EntityDrive;"))
    private EntityDrive onDoWaterDrive(LivingEntity attacker, float roll, int lifetime, Vec3 centerOffset,
                                       boolean critical, double damage, float size) {
        if (attacker.level().isClientSide() || attacker.getRandom().nextFloat() >= 0.25F) return null;
        return WaterDrive.doSlash(attacker, roll, lifetime, centerOffset, critical, 0.6D, size);
    }
}
