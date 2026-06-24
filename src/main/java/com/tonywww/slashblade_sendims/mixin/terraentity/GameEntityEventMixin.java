package com.tonywww.slashblade_sendims.mixin.terraentity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import org.confluence.terraentity.entity.monster.AbstractMonster;
import org.confluence.terraentity.event.GameEntityEvent;
import org.confluence.terraentity.init.TEEffects;
import org.confluence.terraentity.init.entity.TEMonsterEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameEntityEvent.class, remap = false)
public class GameEntityEventMixin {
    @Inject(
            method = "livingDamageEntity(Lnet/minecraftforge/event/entity/living/LivingDamageEvent;)V",
            at = @At("HEAD"),
            remap = false,
            cancellable = true)
    private static void sdbf$disableSoulEaterSpawn(LivingDamageEvent event, CallbackInfo ci) {
        LivingEntity e1 = event.getEntity();
        Level level = event.getEntity().level();
        Entity attacker = event.getSource().getEntity();
        if (level instanceof ServerLevel) {
            if (attacker != null && attacker.getType() == TEMonsterEntities.DECAYEDER.get()) {
                if (!e1.hasEffect(TEEffects.DEMONIC_THOUGHTS.get())) {
                    e1.addEffect(new MobEffectInstance(TEEffects.DEMONIC_THOUGHTS.get(), 200), attacker);
                } else {
                    e1.removeEffect(TEEffects.DEMONIC_THOUGHTS.get());
                    e1.hurt(event.getSource(), 6.0F);

                    e1.removeEffect(TEEffects.DEMONIC_THOUGHTS.get());
                }
            }

            if (attacker != null && (attacker.getType() == TEMonsterEntities.CRIMSLIME.get() || attacker.getType() == TEMonsterEntities.CORRUPT_SLIME.get()) && e1.getRandom().nextFloat() <= 0.25F) {
                e1.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300), attacker);
            }

        }
        ci.cancel();
    }
}
