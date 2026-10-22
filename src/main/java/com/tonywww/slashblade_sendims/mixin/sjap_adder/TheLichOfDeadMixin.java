package com.tonywww.slashblade_sendims.mixin.sjap_adder;

import com.dinzeer.legendreliclib.lib.util.slashblade.AbstractSpecialEffect;
import com.dinzeer.sjapadder.se.kamuy.TheLichOfDead;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TheLichOfDead.class, remap = false)
public abstract class TheLichOfDeadMixin extends AbstractSpecialEffect {

    public TheLichOfDeadMixin(int questionLevel) {
        super(questionLevel);
    }

    @Inject(method = "handleHitEntity", at = @At("HEAD"), cancellable = true)
    private void onHandleHitEntity(SlashBladeEvent.HitEvent event, LivingEntity attacker, CallbackInfo ci) {
        ci.cancel();
        if (attacker.level().isClientSide || !isEffective(attacker)) return;
        if (attacker.getRandom().nextInt(100) >= 15) return;

        int count = 1 + attacker.getRandom().nextInt(3);
        Vec3 backward = attacker.getLookAngle().normalize().reverse();
        for (int i = 0; i < count; i++) {
            float yawOffset = (i - (count - 1) * 0.5F) * 45.0F;
            Vec3 direction = backward.yRot((float) Math.toRadians(yawOffset));
            TheLichOfDead.spawnDeadBall(attacker, event.getTarget(), direction, yawOffset, 0.15F);
        }
    }
}
