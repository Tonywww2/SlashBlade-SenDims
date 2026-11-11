package com.tonywww.slashblade_sendims.mixin.draconicevolution;

import com.brandon3055.draconicevolution.api.modules.entities.ShieldControlEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ShieldControlEntity.class, remap = false)
public interface ShieldControlEntityAccessor {
    /** DE's shield cost weighting: armor-bypassing damage ×3, magic ×2. */
    @Invoker("applyDamageModifiers")
    float sendims$applyDamageModifiers(DamageSource source, float damage);
}
