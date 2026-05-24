package com.tonywww.slashblade_sendims.mixin.lsp;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.royling.lsp.ModCap.BloodAccumulationEvent;
import net.royling.lsp.ModCap.ModCapabilities;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(BloodAccumulationEvent.class)
public class MixinBloodAccumulationEvent {

    @Unique
    private static final Vector3f BLOOD_COLOR = new Vector3f(0.7F, 0.0F, 0.0F);

    @Inject(method = "onLivingTick(Lnet/minecraftforge/event/entity/living/LivingEvent$LivingTickEvent;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void injectOnLivingTick(LivingEvent.LivingTickEvent event, CallbackInfo ci) {
        if (event.getEntity() instanceof LivingEntity && !event.getEntity().level().isClientSide()) {
            LivingEntity entity = event.getEntity();
            entity.getCapability(ModCapabilities.BLOOD_ACCUMULATION_CAPABILITY).ifPresent((bloodAccum) -> {
                if (bloodAccum.getAccumulation() >= 120 && entity.invulnerableTime == 0) {
                    float maxHealth = entity.getMaxHealth();
                    float currHealth = entity.getHealth();
                    float bleedDamage = (maxHealth - currHealth) * 0.2F + 8.0F;
                    UUID lastAttackerUUID = bloodAccum.getLastAttackerUUID();
                    Entity lastAttacker = null;
                    if (lastAttackerUUID != null) {
                        Level patt3181$temp = entity.level();
                        if (patt3181$temp instanceof ServerLevel) {
                            ServerLevel serverLevel = (ServerLevel) patt3181$temp;
                            lastAttacker = serverLevel.getEntity(lastAttackerUUID);
                        }
                    }
                    DamageSource source;
                    if (lastAttacker instanceof LivingEntity livingEntity) {
                        source = livingEntity.damageSources().mobAttack(livingEntity);

                    } else {
                        source = entity.damageSources().mobAttack(entity);
                    }

                    for (int i = 0; i < 4; i++) {
                        entity.hurt(source, bleedDamage / 4);
                        entity.invulnerableTime = 0;

                    }

                    Level patt3905$temp = entity.level();
                    if (patt3905$temp instanceof ServerLevel) {
                        ServerLevel serverLevel = (ServerLevel) patt3905$temp;
                        double x = entity.getX();
                        double y = entity.getY() + (double) entity.getBbHeight() * (double) 0.5F;
                        double z = entity.getZ();
                        DustParticleOptions bloodParticle = new DustParticleOptions(BLOOD_COLOR, 1.0F);
                        int particleCount = Math.min(bloodAccum.getAccumulation() / 5, 60);
                        serverLevel.sendParticles(bloodParticle, x, y, z, particleCount, (double) entity.getBbWidth() * 0.7, (double) entity.getBbHeight() * 0.3, (double) entity.getBbWidth() * 0.7, 0.15);
                        serverLevel.sendParticles(bloodParticle, x, y - (double) entity.getBbHeight() * (double) 0.25F, z, 20, (double) entity.getBbWidth() * 0.4, 0.1, (double) entity.getBbWidth() * 0.4, 0.05);
                    }

                    bloodAccum.clearAccumulation();
                }

                int decayRate = 1;
                if (entity.tickCount % 5 == 0 && bloodAccum.getAccumulation() > 0) {
                    bloodAccum.addAccumulation(-decayRate);
                }

            });
        }
        ci.cancel();
    }
}
