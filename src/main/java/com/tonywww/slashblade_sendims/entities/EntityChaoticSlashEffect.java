package com.tonywww.slashblade_sendims.entities;

import com.mojang.math.Axis;
import com.tonywww.slashblade_sendims.utils.ChaoticAttackManager;
import com.tonywww.slashblade_sendims.utils.ChaoticSlashArtEffects;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.event.handler.FallHandler;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.util.List;

public class EntityChaoticSlashEffect extends EntitySlashEffect {
    public EntityChaoticSlashEffect(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        if (!this.hasBeenShot) {
            this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
            this.hasBeenShot = true;
        }
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        this.baseTick();

        if (this.tickCount == 2) {
            if (this.getMute()) {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F,
                        0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
            } else {
                this.playSound(this.getSlashSound(), 0.8F, 0.625F + 0.1F * this.random.nextFloat());
            }
            if (this.getIsCritical()) {
                this.playSound(this.getHitEntitySound(), 0.2F, 0.4F + 0.25F * this.random.nextFloat());
            }
        }

        if (this.tickCount % 2 == 0 || this.tickCount < 5) {
            spawnSlashParticles();
        }

        if (this.getShooter() != null && this.tickCount % 2 == 0) {
            List<Entity> hits;
            if (!this.getIndirect() && this.getShooter() instanceof LivingEntity attacker) {
                float damage = (float) this.getDamage() * (this.getIsCritical() ? 1.1F : 1.0F);
                hits = ChaoticAttackManager.areaAttack(
                        attacker,
                        this.getKnockBack().action,
                        damage,
                        true,
                        false,
                        true,
                        this.getAlreadyHits()
                );
            } else {
                hits = ChaoticAttackManager.areaAttack(
                        this,
                        this.getKnockBack().action,
                        4.0D,
                        true,
                        false,
                        1.0F,
                        this.getAlreadyHits()
                );
            }

            if (!this.doCycleHit()) {
                this.getAlreadyHits().addAll(hits);
            }
        }

        this.tryDespawn();
    }

    private void spawnSlashParticles() {
        Vec3 start = this.position();
        Vector4f normal = new Vector4f(1.0F, 0.0F, 0.0F, 1.0F);
        Vector4f direction = new Vector4f(0.0F, 0.0F, 1.0F, 1.0F);
        float progress = (float) this.tickCount / this.getLifetime();
        transform(normal, progress);
        transform(direction, progress);

        Vec3 normalVector = new Vec3(normal.x(), normal.y(), normal.z());
        BlockHitResult rayResult = this.level().clip(new ClipContext(
                start.add(normalVector.scale(1.5D)),
                start.add(normalVector.scale(3.0D)),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                null
        ));
        if (this.getShooter() != null
                && !this.getShooter().isInWaterOrRain()
                && rayResult.getType() == HitResult.Type.BLOCK) {
            FallHandler.spawnLandingParticle(this, rayResult.getLocation(), normalVector, 3.0F);
        }

        Vec3 particlePos = start.add(normalVector.scale(this.getBaseSize() * 2.5D));
        addChaosParticle(particlePos, normalVector, new Vec3(direction.x(), direction.y(), direction.z()));

        if (IConcentrationRank.ConcentrationRanks.S.level < this.getRankCode().level) {
            float randomScale = this.random.nextFloat() + 0.5F;
            particlePos = particlePos.add(
                    direction.x() * randomScale,
                    direction.y() * randomScale,
                    direction.z() * randomScale
            );
                    addChaosParticle(particlePos, normalVector, new Vec3(direction.x(), direction.y(), direction.z()));
        }
    }

    private void transform(Vector4f vector, float progress) {
        Axis.YP.rotationDegrees(60.0F + this.getRotationOffset() - 200.0F * progress).transform(vector);
        Axis.ZP.rotationDegrees(this.getRotationRoll()).transform(vector);
        Axis.XP.rotationDegrees(this.getXRot()).transform(vector);
        Axis.YP.rotationDegrees(-this.getYRot()).transform(vector);
    }

    private void addChaosParticle(Vec3 position, Vec3 normal, Vec3 direction) {
        ChaoticSlashArtEffects.spawnSlashParticles(
                this.level(),
                this.random,
                position,
                normal,
                direction,
                this.getBaseSize()
        );
    }
}