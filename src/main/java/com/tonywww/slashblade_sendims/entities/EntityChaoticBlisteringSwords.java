package com.tonywww.slashblade_sendims.entities;

import cn.mmf.slashblade_addon.entity.BlisteringSwordsEntity;
import com.brandon3055.brandonscore.api.TechLevel;
import com.brandon3055.draconicevolution.init.DEDamage;
import com.tonywww.slashblade_sendims.utils.ChaoticSlashArtEffects;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import mods.flammpfeil.slashblade.SlashBladeConfig;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;

public class EntityChaoticBlisteringSwords extends BlisteringSwordsEntity {
    private IntOpenHashSet alreadyHits;
    private int ticksInAir;
    private boolean clientWasFired;

    public EntityChaoticBlisteringSwords(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        if (this.itFired() && this.getVehicle() == null && this.getHitEntity() == null) {
            this.ticksInAir++;
        }
        super.tick();

        boolean fired = this.itFired();
        ChaoticSlashArtEffects.spawnBlisteringSwordParticles(
                this,
                this.random,
                fired,
                !this.clientWasFired && fired
        );
        if (this.level().isClientSide()) {
            this.clientWasFired = fired;
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        MinecraftForge.EVENT_BUS.post(new SlashBladeEvent.SummonedSwordOnHitEntityEvent(this, target));

        int damage = Mth.ceil(this.getDamage());
        if (this.getPierce() > 0) {
            if (this.alreadyHits == null) {
                this.alreadyHits = new IntOpenHashSet(5);
            }
            if (this.alreadyHits.size() >= this.getPierce() + 1) {
                this.burst();
                return;
            }
            this.alreadyHits.add(target.getId());
        }

        if (this.getIsCritical()) {
            damage += this.random.nextInt(damage / 2 + 2);
        }

        Entity shooter = this.getShooter();
        DamageSource source = DEDamage.draconicArrow(
                target.level(),
                null,
                shooter,
                TechLevel.CHAOTIC,
                true
        );
        if (shooter instanceof LivingEntity livingShooter) {
            Entity actualTarget = target instanceof PartEntity<?> part ? part.getParent() : target;
            livingShooter.setLastHurtMob(actualTarget);
        }

        int fireTime = target.getRemainingFireTicks();
        if (this.isOnFire() && !(target instanceof EnderMan)) {
            target.setSecondsOnFire(5);
        }

        target.invulnerableTime = 0;
        float scale = 1.0F;
        if (shooter instanceof LivingEntity livingShooter) {
            scale = (float) (AttackManager.getSlashBladeDamageScale(livingShooter)
                    * SlashBladeConfig.SLASHBLADE_DAMAGE_MULTIPLIER.get());
        }

        if (target.hurt(source, damage * scale * ChaoticSlashArtEffects.DAMAGE_MULTIPLIER)) {
            Entity actualTarget = target instanceof PartEntity<?> part ? part.getParent() : target;
            if (actualTarget instanceof LivingEntity livingTarget) {
                StunManager.setStun(livingTarget);
                if (!this.level().isClientSide() && this.getPierce() <= 0) {
                    this.setHitEntity(actualTarget);
                }
                if (!this.level().isClientSide() && shooter instanceof LivingEntity livingShooter) {
                    EnchantmentHelper.doPostHurtEffects(livingTarget, shooter);
                    EnchantmentHelper.doPostDamageEffects(livingShooter, livingTarget);
                }

                this.affectEntity(livingTarget, this.getPotionEffects(), 1.0D);
                if (livingTarget != shooter
                        && livingTarget instanceof Player
                    && shooter instanceof ServerPlayer serverPlayer) {
                    serverPlayer.playNotifySound(
                            this.getHitEntityPlayerSound(),
                            SoundSource.PLAYERS,
                            0.18F,
                            0.45F
                    );
                }
            }

            this.playSound(this.getHitEntitySound(), 1.0F,
                    1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            if (this.getPierce() <= 0 && (this.getHitEntity() == null || !this.getHitEntity().isAlive())) {
                this.burst();
            }
        } else {
            target.setRemainingFireTicks(fireTime);
            this.ticksInAir = 0;
            if (!this.level().isClientSide() && this.getDeltaMovement().lengthSqr() < 1.0E-7D) {
                if (this.getPierce() <= 1) {
                    this.burst();
                } else {
                    this.setPierce((byte) (this.getPierce() - 1));
                }
            }
        }
    }

    @Nullable
    @Override
    protected EntityHitResult getRayTrace(Vec3 start, Vec3 end) {
        return ProjectileUtil.getEntityHitResult(
                this.level(),
                this,
                start,
                end,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D),
                entity -> entity.canBeHitByProjectile()
                        && !entity.isSpectator()
                        && (entity != this.getShooter() || this.ticksInAir >= 5)
                        && (this.alreadyHits == null || !this.alreadyHits.contains(entity.getId()))
        );
    }

    @Override
    public void resetAlreadyHits() {
        super.resetAlreadyHits();
        if (this.alreadyHits != null) {
            this.alreadyHits.clear();
        }
    }
}