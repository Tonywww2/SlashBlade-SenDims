package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.legendreliclib.lib.util.slashblade.SlashBladeUtil;
import com.dinzeer.sjapadder.entity.DeadBall;
import com.dinzeer.sjapadder.sa.DeadBallSa;
import com.tonywww.slashblade_sendims.entity.DeadBallReturnDamage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DeadBallSa.class, remap = false)
public abstract class DeadBallSaMixin {
    // Intercept only the SA factory call; the SE retains the native factory.
    // Preserve the native spawn pose, color, damage snapshot and scheduling.
    @Redirect(method = "lambda$doSlash$0", at = @At(value = "INVOKE",
            target = "Lcom/dinzeer/sjapadder/se/kamuy/TheLichOfDead;spawnDeadBall(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;FF)V"),
            require = 1, allow = 1, remap = false)
    private static void sendims$spawnSlashArtBall(LivingEntity owner, LivingEntity target,
                                                 Vec3 direction, float yawOffset, float damage) {
        if (owner.level().isClientSide) return;
        var state = SlashBladeUtil.getState(owner.getMainHandItem());
        DeadBall ball = new DeadBall(owner.level(), owner, target);
        Vec3 spawnPos = target.position().add(0, target.getEyeHeight() * 0.8, 0)
                .add(direction.scale(1.2));
        ball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        float yaw = target.getYRot() + 180.0F + yawOffset;
        ball.setYRot(yaw);
        ball.setYHeadRot(yaw);
        double attack = owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ball.setDamage((float) (damage * attack));
        ((DeadBallReturnDamage) ball).sendims$setSlashArtReturnDamage((float) (0.3F * attack));
        ball.setColor(state.getColorCode());
        owner.level().addFreshEntity(ball);
    }
}
