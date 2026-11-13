package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.entity.DeadBall;
import java.util.Deque;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Dedicated server only (SenDimsMixinConfigPlugin skips "*ServerTrailMixin" elsewhere). sjap_adder marks
 * DeadBall.trailPositions as @OnlyIn(CLIENT), so the server strips the field while the
 * constructor still assigns it, throwing NoSuchFieldError whenever a ball spawns.
 * Every other access is behind level().isClientSide, so only the assignment is dropped.
 */
@Mixin(value = DeadBall.class, remap = false)
public abstract class DeadBallServerTrailMixin {
    @Redirect(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",
            at = @At(value = "FIELD", target = "Lcom/dinzeer/sjapadder/entity/DeadBall;trailPositions:Ljava/util/Deque;",
                    opcode = Opcodes.PUTFIELD),
            require = 1, allow = 1, remap = false)
    private void sendims$skipClientTrail(DeadBall ball, Deque<?> trail) {
    }
}
