package com.tonywww.slashblade_sendims.entities;

import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.VectorHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

public class EntityStableDrive extends EntityDrive {

    public EntityStableDrive(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    static float calculateDamage(double recordedDamage, int rankLevel, int refine) {
        return (float) (recordedDamage + (rankLevel + (double) refine) / 2.0D);
    }

    public static void doSlash(LivingEntity attacker, float roll, int lifetime, Vec3 centerOffset,
            boolean critical, double damage, KnockBacks knockback,
            float minSpeed, float maxSpeed, int additionalCount) {
        if (attacker.level().isClientSide())
            return;

        int color = attacker.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.getEffectColor().getRGB())
                .orElse(0xFF3333FF);
        Vec3 position = attacker.position()
                .add(0.0D, attacker.getEyeHeight() * 0.75D, 0.0D)
                .add(attacker.getLookAngle().scale(0.3F));
        position = position
                .add(VectorHelper.getVectorForRotation(-90.0F, attacker.getViewYRot(0.0F)).scale(centerOffset.y))
                .add(VectorHelper.getVectorForRotation(0.0F, attacker.getViewYRot(0.0F) + 90.0F).scale(centerOffset.z))
                .add(attacker.getLookAngle().scale(centerOffset.z));

        for (int index = 0; index <= additionalCount; index++) {
            EntityStableDrive drive = new EntityStableDrive(SBSDEntities.STABLE_DRIVE.get(), attacker.level());
            float speed = Mth.randomBetween(drive.level().getRandom(), minSpeed, maxSpeed);

            drive.setPos(position.x, position.y, position.z);
            drive.setDamage(damage);
            drive.setSpeed(speed);
            Vec3 lookAngle = attacker.getLookAngle();
            drive.shoot(lookAngle.x, lookAngle.y, lookAngle.z, speed, 0.0F);
            drive.setOwner(attacker);
            drive.setRotationRoll(roll);
            drive.setColor(color);
            drive.setIsCritical(critical);
            drive.setKnockBack(knockback);
            drive.setLifetime(lifetime);
            attacker.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                    .ifPresent(rank -> drive.setRank(rank.getRankLevel(attacker.level().getGameTime())));
            attacker.level().addFreshEntity(drive);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity targetEntity = entityHitResult.getEntity();
        Entity shooter = this.getShooter();
        DamageSource damageSource;
        if (shooter == null) {
            damageSource = this.damageSources().indirectMagic(this, this);
        } else {
            damageSource = this.damageSources().indirectMagic(this, shooter);
            if (shooter instanceof LivingEntity living) {
                Entity hitTarget = targetEntity;
                if (targetEntity instanceof PartEntity<?> part) {
                    hitTarget = part.getParent();
                }
                living.setLastHurtMob(hitTarget);
            }
        }

        int fireTime = targetEntity.getRemainingFireTicks();
        if (this.isOnFire() && !(targetEntity instanceof EnderMan)) {
            targetEntity.setSecondsOnFire(5);
        }

        targetEntity.invulnerableTime = 0;
        int rankLevel = IConcentrationRank.ConcentrationRanks.NONE.level;
        int refine = 0;
        if (this.getOwner() instanceof Player player) {
            rankLevel = player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                    .map(rank -> rank.getRank(player.level().getGameTime()).level)
                    .orElse(IConcentrationRank.ConcentrationRanks.NONE.level);
            refine = player.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                    .map(ISlashBladeState::getRefine)
                    .orElse(0);
        }
        float damageValue = calculateDamage(this.getDamage(), rankLevel, refine);

        if (targetEntity.hurt(damageSource, damageValue)) {
            Entity hitTarget = targetEntity;
            if (targetEntity instanceof PartEntity<?> part) {
                hitTarget = part.getParent();
            }

            if (hitTarget instanceof LivingEntity livingTarget) {
                StunManager.setStun(livingTarget);
                if (!this.level().isClientSide() && shooter instanceof LivingEntity livingShooter) {
                    EnchantmentHelper.doPostHurtEffects(livingTarget, livingShooter);
                    EnchantmentHelper.doPostDamageEffects(livingShooter, livingTarget);
                }

                this.affectEntity(livingTarget, this.getPotionEffects(), 1.0D);
                if (livingTarget != shooter && livingTarget instanceof Player
                        && shooter instanceof ServerPlayer serverPlayer) {
                    serverPlayer.playNotifySound(this.getHitEntityPlayerSound(), SoundSource.PLAYERS, 0.18F, 0.45F);
                }
            }

            this.playSound(this.getHitEntitySound(), 1.0F,
                    1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        } else {
            targetEntity.setRemainingFireTicks(fireTime);
        }
    }
}