package com.tonywww.slashblade_sendims.utils;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/** Shared non-magical true damage. Armor/effect/shield bypass is defined by damage-type tags. */
public final class TrueDamage {
    public static final ResourceKey<DamageType> TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, SenDims.prefix("true_damage"));
    private static final String ACTIVE = "sbsd.damage.true_damage_active";

    private TrueDamage() {}

    public static DamageSource source(LivingEntity attacker) {
        return new DamageSource(attacker.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TYPE), attacker);
    }

    public static boolean isTrueDamage(DamageSource source) {
        return source.is(TYPE);
    }

    /** Native hurt path retains attribution, absorption, death and cancellable Forge damage events. */
    public static boolean deal(LivingEntity attacker, LivingEntity target, float amount) {
        if (!Float.isFinite(amount) || amount <= 0 || target == attacker || !target.isAlive()) return false;
        CompoundTag data = attacker.getPersistentData();
        if (data.getBoolean(ACTIVE)) return false;
        data.putBoolean(ACTIVE, true);
        try {
            target.invulnerableTime = 0;
            return target.hurt(source(attacker), amount);
        } finally {
            data.remove(ACTIVE);
        }
    }
}
