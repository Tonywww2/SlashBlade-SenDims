package com.tonywww.slashblade_sendims.entities;

import com.mojang.math.Axis;
import com.tonywww.slashblade_sendims.utils.MobAttackManager;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.event.handler.FallHandler;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.util.List;

public class EntityMobSlashEffect extends EntitySlashEffect {
    public EntityMobSlashEffect(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount == 2) {
            if (!this.getMute()) {
                this.playSound(this.getSlashSound(), 0.8F, 0.625F + 0.1F * this.random.nextFloat());
            } else {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
            }

            if (this.getIsCritical()) {
                this.playSound(this.getHitEntitySound(), 0.2F, 0.4F + 0.25F * this.random.nextFloat());
            }
        }

        float ratio;
        if (this.tickCount % 2 == 0 || this.tickCount < 5) {
            Vec3 start = this.position();
            Vector4f normal = new Vector4f(1.0F, 0.0F, 0.0F, 1.0F);
            Vector4f dir = new Vector4f(0.0F, 0.0F, 1.0F, 1.0F);
            ratio = (float) this.tickCount / (float) this.getLifetime();
            Axis.YP.rotationDegrees(60.0F + this.getRotationOffset() - 200.0F * ratio).transform(normal);
            Axis.ZP.rotationDegrees(this.getRotationRoll()).transform(normal);
            Axis.XP.rotationDegrees(this.getXRot()).transform(normal);
            Axis.YP.rotationDegrees(-this.getYRot()).transform(normal);
            Axis.YP.rotationDegrees(60.0F + this.getRotationOffset() - 200.0F * ratio).transform(dir);
            Axis.ZP.rotationDegrees(this.getRotationRoll()).transform(dir);
            Axis.XP.rotationDegrees(this.getXRot()).transform(dir);
            Axis.YP.rotationDegrees(-this.getYRot()).transform(dir);
            Vec3 normal3d = new Vec3((double) normal.x(), (double) normal.y(), (double) normal.z());
            BlockHitResult rayResult = this.getCommandSenderWorld().clip(new ClipContext(start.add(normal3d.scale(1.5)), start.add(normal3d.scale(3.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, (Entity) null));
            if (this.getShooter() != null && !this.getShooter().isInWaterOrRain() && rayResult.getType() == HitResult.Type.BLOCK) {
                FallHandler.spawnLandingParticle(this, rayResult.getLocation(), normal3d, 3.0F);
            }

            if (IConcentrationRank.ConcentrationRanks.S.level < this.getRankCode().level) {
                Vec3 vec3 = start.add(normal3d.scale((double) this.getBaseSize() * 2.5));
                this.level().addParticle(ParticleTypes.CRIT, vec3.x(), vec3.y(), vec3.z(), (double) (dir.x() + normal.x()), (double) (dir.y() + normal.y()), (double) (dir.z() + normal.z()));
                float randScale = this.random.nextFloat() * 1.0F + 0.5F;
                vec3 = vec3.add((double) (dir.x() * randScale), (double) (dir.y() * randScale), (double) (dir.z() * randScale));
                this.level().addParticle(ParticleTypes.CRIT, vec3.x(), vec3.y(), vec3.z(), (double) (dir.x() + normal.x()), (double) (dir.y() + normal.y()), (double) (dir.z() + normal.z()));
            }
        }

        if (this.getShooter() != null && this.tickCount % 2 == 0) {
            boolean forceHit = true;
            List<Entity>  hits;
            if (!this.getIndirect() && this.getShooter() instanceof LivingEntity) {
                LivingEntity shooter = (LivingEntity) this.getShooter();
                ratio = (float) this.getDamage() * (this.getIsCritical() ? 1.1F : 1.0F);
                hits = MobAttackManager.areaAttack(shooter, KnockBacks.smash.action, ratio, forceHit, false, true, this.getAlreadyHits());
            } else {
                hits = MobAttackManager.areaAttack(this, KnockBacks.smash.action, 4.0, forceHit, false, this.getAlreadyHits());
            }

            if (!this.doCycleHit()) {
                this.getAlreadyHits().addAll(hits);
            }
        }

        this.tryDespawn();
    }
}
