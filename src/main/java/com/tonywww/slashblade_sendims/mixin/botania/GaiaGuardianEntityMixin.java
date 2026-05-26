package com.tonywww.slashblade_sendims.mixin.botania;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vazkii.botania.common.entity.GaiaGuardianEntity;
import vazkii.botania.common.helper.VecHelper;

@Mixin(value = GaiaGuardianEntity.class)
public abstract class GaiaGuardianEntityMixin extends Mob {

    @Shadow(remap = false)
    private int tpDelay;

    @Shadow(remap = false)
    private boolean spawnPixies;

    protected GaiaGuardianEntityMixin(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectActuallyHurt(DamageSource source, float amount, CallbackInfo ci) {
        super.actuallyHurt(source, amount);
        Entity attacker = source.getDirectEntity();
        if (attacker != null) {
            Vec3 thisVector = VecHelper.fromEntityCenter(this);
            Vec3 playerVector = VecHelper.fromEntityCenter(attacker);
            Vec3 motionVector = thisVector.subtract(playerVector).normalize().scale(0.75d);
            if (this.getHealth() > 0.0F) {
                this.setDeltaMovement(-motionVector.x, 0.5F, -motionVector.z);
                this.tpDelay = 4;
                this.spawnPixies = true;
            }
        }

        this.invulnerableTime = Math.max(this.invulnerableTime, 20);
        ci.cancel();
    }

}
