package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.sjapadder.entity.WhirlingSwordEntity;
import java.util.Deque;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Same dedicated-server fix as DeadBallServerTrailMixin; updateTrailPositions checks isClientSide first. */
@Mixin(value = WhirlingSwordEntity.class, remap = false)
public abstract class WhirlingSwordServerTrailMixin {
    @Redirect(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",
            at = @At(value = "FIELD", target = "Lcom/dinzeer/sjapadder/entity/WhirlingSwordEntity;trailPositions:Ljava/util/Deque;",
                    opcode = Opcodes.PUTFIELD),
            require = 1, allow = 1, remap = false)
    private void sendims$skipClientTrail(WhirlingSwordEntity sword, Deque<?> trail) {
    }
}
