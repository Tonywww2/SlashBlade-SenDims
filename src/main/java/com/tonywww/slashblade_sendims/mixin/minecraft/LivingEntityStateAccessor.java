package com.tonywww.slashblade_sendims.mixin.minecraft;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityStateAccessor {
    /** Game time of the last damage this entity took (vanilla {@code lastDamageStamp}). */
    @Accessor("lastDamageStamp")
    long sendims$getLastDamageStamp();
}
