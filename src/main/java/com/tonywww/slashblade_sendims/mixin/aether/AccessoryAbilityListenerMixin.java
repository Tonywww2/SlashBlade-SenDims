package com.tonywww.slashblade_sendims.mixin.aether;

import com.aetherteam.aether.event.listeners.abilities.AccessoryAbilityListener;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessoryAbilityListener.class)
public class AccessoryAbilityListenerMixin {
    @Inject(method = "onProjectileImpact(Lnet/minecraftforge/event/entity/ProjectileImpactEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sdbf$deflectProjectile(ProjectileImpactEvent event, CallbackInfo ci) {
        if (event.getRayTraceResult() == null) {
            ci.cancel();
        }
    }
}
