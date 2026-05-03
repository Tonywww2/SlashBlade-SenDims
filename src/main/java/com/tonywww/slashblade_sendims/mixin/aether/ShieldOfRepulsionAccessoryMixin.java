package com.tonywww.slashblade_sendims.mixin.aether;

import com.aetherteam.aether.item.accessories.abilities.ShieldOfRepulsionAccessory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShieldOfRepulsionAccessory.class)
public class ShieldOfRepulsionAccessoryMixin {
    @Inject(method = "deflectProjectile", at = @At("HEAD"), cancellable = true)
    private static void sdbf$deflectProjectile(ProjectileImpactEvent event, HitResult hitResult, Projectile projectile, CallbackInfo ci) {
        if (hitResult == null) {
            ci.cancel();
        }
    }
}
