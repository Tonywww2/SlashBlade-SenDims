package com.tonywww.slashblade_sendims.utils;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.init.DEDamage;
import com.google.common.collect.Lists;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.util.AttackHelper;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantments;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public final class ChaoticAttackManager {
    private ChaoticAttackManager() {
    }

    public static <T extends Entity & IShootable> List<Entity> areaAttack(
            T source,
            Consumer<LivingEntity> beforeHit,
            double reach,
            boolean forceHit,
            boolean resetHit,
            float comboRatio,
            @Nullable List<Entity> excluded
    ) {
        List<Entity> targets = Lists.newArrayList();
        if (source.level().isClientSide()) {
            return targets;
        }

        targets = TargetSelector.getTargettableEntitiesWithinAABB(source.level(), reach, source);
        if (excluded != null) {
            targets.removeAll(excluded);
        }

        for (Entity target : targets) {
            if (target instanceof LivingEntity livingTarget) {
                beforeHit.accept(livingTarget);
            }

            double damage = source.getDamage();
            if (source.getShooter() instanceof LivingEntity attacker) {
                if (!(source instanceof EntitySlashEffect)) {
                    int powerLevel = attacker.getMainHandItem().getEnchantmentLevel(Enchantments.POWER_ARROWS);
                    damage += powerLevel * 0.1D;
                }

                damage *= attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
                damage += AttackHelper.getRankBonus(attacker);
                damage *= comboRatio * AttackManager.getSlashBladeDamageScale(attacker)
                        * SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get();
            }

            AttackManager.doAttackWith(
                    DEDamage.draconicArrow(
                            source.level(),
                            source,
                            source.getShooter(),
                            TechLevel.CHAOTIC,
                            false
                    ),
                    (float) damage,
                    target,
                    forceHit,
                    resetHit
            );
        }

        return targets;
    }
}