package com.tonywww.slashblade_sendims.mixin.botania;

import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.entity.ManaBurstEntity;

@Mixin(value = ManaBurstEntity.class)
public abstract class ManaBurstEntityMixin extends ThrowableProjectile {

    protected ManaBurstEntityMixin(EntityType<? extends ThrowableProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "onHitEntity(Lnet/minecraft/world/phys/EntityHitResult;)V", at = @At("HEAD"))
    private void injectOnHitEntity(EntityHitResult hit, CallbackInfo ci) {
        if (this.level().isClientSide()) return;
        Entity target = hit.getEntity();
        Entity attacker = this.getOwner();
        if (attacker instanceof LivingEntity liveAttacker) {
            AttackManager.doMeleeAttack(liveAttacker, target, true, false, 0.15f);
        }
    }
}

