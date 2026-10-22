package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.entity.DeadBall;
import com.tonywww.slashblade_sendims.entity.DeadBallReturnDamage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DeadBall.class, remap = false)
public abstract class DeadBallMixin implements DeadBallReturnDamage {
    @Unique
    private float sendims$slashArtReturnDamage = Float.NaN;

    @Override
    public void sendims$setSlashArtReturnDamage(float damage) {
        this.sendims$slashArtReturnDamage = damage;
    }

    // Only the homing return hit is overridden. Outgoing collisions and the
    // native explosion continue to read the original damage stored on the ball.
    @Redirect(method = "onHitTarget", at = @At(value = "INVOKE",
            target = "Lcom/dinzeer/sjapadder/entity/DeadBall;getDamage()F"),
            require = 1, allow = 1, remap = false)
    private float sendims$returnHitDamage(DeadBall ball) {
        return Float.isNaN(this.sendims$slashArtReturnDamage)
                ? ball.getDamage() : this.sendims$slashArtReturnDamage;
    }
}
