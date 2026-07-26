package com.tonywww.slashblade_sendims.utils;

import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.init.DEDamage;
import com.google.common.collect.Lists;
import com.tonywww.slashblade_sendims.entities.EntityChaoticSlashEffect;
import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.ability.ArrowReflector;
import mods.flammpfeil.slashblade.ability.TNTExtinguisher;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackHelper;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TargetSelector;
import mods.flammpfeil.slashblade.util.VectorHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public final class ChaoticAttackManager {
    private ChaoticAttackManager() {
    }

    @Nullable
    public static EntityChaoticSlashEffect doSlash(
            LivingEntity attacker,
            float roll,
            Vec3 centerOffset,
            boolean mute,
            boolean critical,
            double comboRatio
    ) {
        if (attacker.level().isClientSide()) {
            return null;
        }

        ItemStack blade = attacker.getMainHandItem();
        return blade.getCapability(ItemSlashBlade.BLADESTATE).map(state -> {
            SlashBladeEvent.DoSlashEvent event = new SlashBladeEvent.DoSlashEvent(
                    blade,
                    state,
                    attacker,
                    roll,
                    critical,
                    comboRatio,
                    KnockBacks.cancel
            );
            if (MinecraftForge.EVENT_BUS.post(event)) {
                return null;
            }

            Vec3 position = attacker.position()
                    .add(0.0D, attacker.getEyeHeight() * 0.75D, 0.0D)
                    .add(attacker.getLookAngle().scale(0.3F));
            position = position
                    .add(VectorHelper.getVectorForRotation(-90.0F, attacker.getViewYRot(0.0F)).scale(centerOffset.y))
                    .add(VectorHelper.getVectorForRotation(0.0F, attacker.getViewYRot(0.0F) + 90.0F).scale(centerOffset.z))
                    .add(attacker.getLookAngle().scale(centerOffset.z));

            EntityChaoticSlashEffect slash = new EntityChaoticSlashEffect(
                    SBSDEntities.CHAOTIC_SLASH_EFFECT.get(),
                    attacker.level()
            );
            slash.setPos(position.x, position.y, position.z);
            slash.setOwner(event.getUser());
            slash.setRotationRoll(event.getRoll());
            slash.setYRot(attacker.getYRot());
            slash.setXRot(0.0F);
            slash.setColor(ChaoticSlashArtEffects.PRIMARY_COLOR);
            slash.setBaseSize(ChaoticSlashArtEffects.VISUAL_SCALE);
            slash.setMute(mute);
            slash.setIsCritical(event.isCritical());
            slash.setDamage(event.getDamage());
            slash.setKnockBack(event.getKnockback());
            attacker.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                    .ifPresent(rank -> slash.setRank(rank.getRankLevel(attacker.level().getGameTime())));
            attacker.level().addFreshEntity(slash);
            return slash;
        }).orElse(null);
    }

    public static List<Entity> areaAttack(
            LivingEntity attacker,
            Consumer<LivingEntity> beforeHit,
            float comboRatio,
            boolean forceHit,
            boolean resetHit,
            boolean mute,
            @Nullable List<Entity> excluded
    ) {
        List<Entity> targets = Lists.newArrayList();
        if (!attacker.level().isClientSide()) {
            targets = TargetSelector.getTargettableEntitiesWithinAABB(attacker.level(), attacker);
            if (excluded != null) {
                targets.removeAll(excluded);
            }

            for (Entity target : targets) {
                if (target instanceof LivingEntity livingTarget) {
                    beforeHit.accept(livingTarget);
                }
                doMeleeAttack(attacker, target, forceHit, resetHit, comboRatio);
            }
        }

        if (!mute) {
            attacker.level().playSound(
                    null,
                    attacker.getX(),
                    attacker.getY(),
                    attacker.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    0.5F,
                    0.4F / (attacker.getRandom().nextFloat() * 0.4F + 0.8F)
            );
        }
        return targets;
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

        targets = Lists.newArrayList(
                TargetSelector.getTargettableEntitiesWithinAABB(source.level(), reach, source)
        );

        // TargetSelector only yields living entities (and their multipart parts). Chaotic attacks
        // must also strike non-living, projectile-hittable entities such as the Draconic Guardian
        // Crystal, mirroring how the chaotic summoned swords already hit them.
        for (Entity extra : source.level().getEntitiesOfClass(
                Entity.class,
                source.getBoundingBox().inflate(reach),
                entity -> entity.canBeHitByProjectile()
                        && !(entity instanceof LivingEntity)
                        && !(entity instanceof PartEntity<?>)
                        && entity != source
                        && entity != source.getShooter()
        )) {
            if (!targets.contains(extra)) {
                targets.add(extra);
            }
        }

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
                            target.level(),
                            null,
                            source.getShooter(),
                            TechLevel.CHAOTIC,
                            true
                    ),
                    (float) damage * ChaoticSlashArtEffects.DAMAGE_MULTIPLIER,
                    target,
                    forceHit,
                    resetHit
            );
        }

        return targets;
    }

    private static void doMeleeAttack(
            LivingEntity attacker,
            Entity target,
            boolean forceHit,
            boolean resetHit,
            float comboRatio
    ) {
        AttackManager.doManagedAttack(entity -> attacker.getMainHandItem()
                .getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(state -> {
                    try {
                        state.setOnClick(true);
                        attack(attacker, entity, comboRatio);
                    } finally {
                        state.setOnClick(false);
                    }
                }), target, forceHit, resetHit);
        ArrowReflector.doReflect(target, attacker);
        TNTExtinguisher.doExtinguishing(target, attacker);
    }

    private static void attack(LivingEntity attacker, Entity target, float comboRatio) {
        if (attacker instanceof Player player && !ForgeHooks.onPlayerAttackTarget(player, target)) {
            return;
        }
        if (!target.isAttackable() || target.skipAttackInteraction(attacker)) {
            return;
        }

        boolean critical = AttackHelper.isCriticalHit(attacker, target);
        double damage = AttackHelper.calculateTotalDamage(attacker, target, comboRatio, critical);
        if (damage <= 0.0D) {
            return;
        }

        float knockback = AttackHelper.calculateKnockback(attacker);
        AttackHelper.FireAspectResult fireAspect = AttackHelper.handleFireAspect(attacker, target);
        Vec3 originalMotion = target.getDeltaMovement();
        DamageSource source = DEDamage.draconicArrow(
                target.level(),
                null,
                attacker,
                TechLevel.CHAOTIC,
                true
        );
        if (target.hurt(source, (float) damage * ChaoticSlashArtEffects.DAMAGE_MULTIPLIER)) {
            AttackHelper.applyKnockback(attacker, target, knockback);
            AttackHelper.restoreTargetMotionIfNeeded(target, originalMotion);
            AttackHelper.playAttackEffects(attacker, target, critical);
            AttackHelper.handleEnchantmentsAndDurability(attacker, target);
            AttackHelper.handlePostAttackEffects(attacker, target, fireAspect);
        } else {
            AttackHelper.handleFailedAttack(attacker, target, fireAspect);
        }
    }
}