package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.tonywww.slashblade_sendims.compat.draconicevolution.DraconicShieldFinalStage;
import com.tonywww.slashblade_sendims.compat.draconicevolution.ShieldHurtFrame;
import com.tonywww.slashblade_sendims.compat.draconicevolution.ShieldHurtTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When the Draconic shield takes a whole hit at the final damage step, the rest of {@code hurt} is
 * skipped: cooldown is restored, no hurt animation, knockback, hurt sound or attacker tracking, and
 * {@code hurt} returns false so the attacker's on-hit effects do not run, as with DE's old attack-stage
 * block. Applied only with Draconic Evolution installed (SenDimsMixinConfigPlugin).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDraconicShieldMixin implements ShieldHurtTracker {
    @Shadow protected float lastHurt;
    @Shadow protected void hurtArmor(DamageSource source, float amount) {}

    @Unique private ShieldHurtFrame sendims$shieldFrame;
    @Unique private int sendims$priorInvulnerableTime;
    @Unique private float sendims$priorLastHurt;

    /** The cooldown check, the only read of invulnerableTime in hurt; both branches call actuallyHurt next. */
    @Inject(method = "hurt", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = Opcodes.GETFIELD))
    private void sendims$capturePriorCooldown(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        sendims$priorInvulnerableTime = ((LivingEntity) (Object) this).invulnerableTime;
        sendims$priorLastHurt = lastHurt;
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void sendims$openShieldFrame(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        sendims$shieldFrame = DraconicShieldFinalStage.open(sendims$shieldFrame,
                sendims$priorInvulnerableTime, sendims$priorLastHurt);
    }

    @Inject(method = "hurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void sendims$closeShieldFrame(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ShieldHurtFrame frame = sendims$shieldFrame;
        if (frame == null) return;
        sendims$shieldFrame = frame.outer();
        if (!DraconicShieldFinalStage.close((LivingEntity) (Object) this, frame)) return;
        ((LivingEntity) (Object) this).invulnerableTime = frame.priorInvulnerableTime();
        lastHurt = frame.priorLastHurt();
        cir.setReturnValue(false);
    }

    @Override
    public ShieldHurtFrame sendims$shieldFrame() {
        return sendims$shieldFrame;
    }

    @Override
    public void sendims$hurtArmorNow(DamageSource source, float amount) {
        hurtArmor(source, amount);
    }
}
