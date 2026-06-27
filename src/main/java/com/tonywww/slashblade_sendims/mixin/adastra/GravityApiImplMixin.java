package com.tonywww.slashblade_sendims.mixin.adastra;

import earth.terrarium.adastra.common.systems.GravityApiImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GravityApiImpl.class, remap = false)
public class GravityApiImplMixin {

    @Unique
    private static final Float SBS$NORMAL_GRAVITY = 1.0F;

    @Inject(method = "getGravity(Lnet/minecraft/world/entity/Entity;)F", at = @At("HEAD"), cancellable = true, remap = false)
    private void sbs$forceNormalGravityForNonPlayerLiving(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (entity instanceof LivingEntity && !(entity instanceof Player)) {
            cir.setReturnValue(SBS$NORMAL_GRAVITY);
        }
    }
}
