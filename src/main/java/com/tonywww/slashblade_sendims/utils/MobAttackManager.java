package com.tonywww.slashblade_sendims.utils;

import com.google.common.collect.Lists;
import com.tonywww.slashblade_sendims.entities.EntityMobSlashEffect;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.ArrowReflector;
import mods.flammpfeil.slashblade.ability.TNTExtinguisher;
import mods.flammpfeil.slashblade.entity.EntityAbstractSummonedSword;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;


public class MobAttackManager {

    public static EntityMobSlashEffect doSlash(LivingEntity entity, float roll, int colorCode, Vec3 centerOffset, boolean mute, boolean critical, double comboRatio, KnockBacks knockback) {
        if (entity.level().isClientSide()) {
            return null;
        } else {
            Vec3 pos = entity.position().add(0d, entity.getEyeHeight() * 0.75d, 0d).add(entity.getLookAngle().scale(0.3f));
            pos = pos.add(VectorHelper.getVectorForRotation(-90.0F, entity.getViewYRot(0.0F)).scale(centerOffset.y)).add(VectorHelper.getVectorForRotation(0.0F, entity.getViewYRot(0.0F) + 90.0F).scale(centerOffset.z)).add(entity.getLookAngle().scale(centerOffset.z));
            EntityMobSlashEffect jc = new EntityMobSlashEffect(SlashBlade.RegistryEvents.SlashEffect, entity.level());
            jc.setPos(pos.x, pos.y, pos.z);
            jc.setOwner(entity);
            jc.setRotationRoll(roll);
            jc.setYRot(entity.getYRot() - 0.5F);
            jc.setXRot(0.0F);
            jc.setColor(colorCode);
            jc.setMute(mute);
            jc.setIsCritical(critical);
            jc.setDamage(comboRatio);
            jc.setKnockBack(knockback);
            jc.setRank(7.0f);
            entity.level().addFreshEntity(jc);
            return jc;
        }
    }

    public static <E extends Entity & IShootable> List<Entity> areaAttack(E owner, Consumer<LivingEntity> beforeHit, double reach, boolean forceHit, boolean resetHit, List<Entity> exclude) {
        return areaAttack(owner, beforeHit, reach, forceHit, resetHit, 1.0F, exclude);
    }

    public static <E extends Entity & IShootable> List<Entity> areaAttack(E owner, Consumer<LivingEntity> beforeHit, double reach, boolean forceHit, boolean resetHit, float comboRatio, List<Entity> exclude) {
        List<Entity> founds = Lists.newArrayList();
        if (!owner.level().isClientSide()) {
            founds = MobTargetSelector.getTargettableEntitiesWithinAABB(owner.level(), reach, owner);
            if (exclude != null) {
                founds.removeAll(exclude);
            }

            Entity entity;
            double baseAmount;
            for (Iterator<Entity> var9 = founds.iterator(); var9.hasNext(); doAttackWith(owner.damageSources().indirectMagic(owner, ((IShootable) owner).getShooter()), (float) baseAmount, entity, forceHit, resetHit)) {
                entity = var9.next();
                if (entity instanceof LivingEntity living) {
                    beforeHit.accept(living);
                }

                baseAmount = owner.getDamage();
                Entity var14 = owner.getShooter();
                if (var14 instanceof LivingEntity living) {
                    if (!(owner instanceof EntityMobSlashEffect)) {
                        int powerLevel = living.getMainHandItem().getEnchantmentLevel(Enchantments.POWER_ARROWS);
                        baseAmount += (double) powerLevel * 0.1;
                    }

                    baseAmount *= living.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    baseAmount += AttackHelper.getRankBonus(living);
                    baseAmount *= (comboRatio * AttackManager.getSlashBladeDamageScale(living));
                }
            }
        }

        return founds;
    }

    public static List<Entity> areaAttack(LivingEntity owner, Consumer<LivingEntity> beforeHit, float comboRatio, boolean forceHit, boolean resetHit, boolean mute, List<Entity> exclude) {
        List<Entity> founds = Lists.newArrayList();
        if (!owner.level().isClientSide()) {
            founds = MobTargetSelector.getTargettableEntitiesWithinAABB(owner.level(), owner);
            if (exclude != null) {
                founds.removeAll(exclude);
            }

            Entity entity;
            for (Iterator<Entity> var8 = founds.iterator(); var8.hasNext(); MobAttackManager.doMeleeAttack(owner, entity, forceHit, resetHit, comboRatio)) {
                entity = var8.next();
                if (entity instanceof LivingEntity living) {
                    beforeHit.accept(living);
                }
            }
        }

        if (!mute) {
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5F, 0.4F / (owner.getRandom().nextFloat() * 0.4F + 0.8F));
        }

        return founds;
    }

    public static void doAttackWith(DamageSource src, float amount, Entity target, boolean forceHit, boolean resetHit) {
        if (!(target instanceof EntityAbstractSummonedSword)) {
            MobAttackManager.doManagedAttack((t) -> {
                t.hurt(src, amount);
            }, target, forceHit, resetHit);
        }
    }

    public static void doMeleeAttack(LivingEntity attacker, Entity target, boolean forceHit, boolean resetHit, float comboRatio) {
        MobAttackManager.doManagedAttack((t) -> {
            AttackHelper.attack(attacker, t, comboRatio);
        }, target, forceHit, resetHit);
        ArrowReflector.doReflect(target, attacker);
        TNTExtinguisher.doExtinguishing(target, attacker);
    }

    public static void doManagedAttack(Consumer<Entity> attack, Entity target, boolean forceHit, boolean resetHit) {
        if (forceHit) {
            target.invulnerableTime = 0;
        }

        attack.accept(target);
        if (resetHit) {
            target.invulnerableTime = 0;
        }

    }
}
