package com.tonywww.slashblade_sendims.entities;

import com.tonywww.slashblade_sendims.mixin.EntityAbstractSummonedSwordAccessor;
import com.tonywww.slashblade_sendims.utils.MobTargetSelector;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.Projectile;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.VectorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class EntityMobDrive extends EntityDrive {
    public EntityMobDrive(EntityType<? extends Projectile> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public static EntityMobDrive doSlash(LivingEntity mobIn, float roll, float yRot, int lifetime, int colorCode, Vec3 centerOffset,
                                      boolean critical, double damage, KnockBacks knockback, float speed, float size) {
        if (mobIn.level().isClientSide())
            return null;

        Vec3 lookAngle = mobIn.getLookAngle();
        Vec3 pos = mobIn.position().add(0.0D, (double) mobIn.getEyeHeight() * 0.75D, 0.0D)
                .add(lookAngle.scale(0.3f));

        pos = pos.add(VectorHelper.getVectorForRotation(-90.0F, mobIn.getViewYRot(0)).scale(centerOffset.y))
                .add(VectorHelper.getVectorForRotation(0, mobIn.getViewYRot(0) + 90).scale(centerOffset.z))
                .add(lookAngle.scale(centerOffset.z));
        EntityMobDrive drive = new EntityMobDrive(SlashBlade.RegistryEvents.Drive, mobIn.level());

        drive.setPos(pos.x, pos.y, pos.z);
        drive.setDamage(damage);
        drive.setSpeed(speed);
        drive.setBaseSize(size);

        var resultAngle = lookAngle.yRot(yRot);

        drive.shoot(resultAngle.x, resultAngle.y, resultAngle.z, drive.getSpeed(),
                0);

        drive.setOwner(mobIn);
        drive.setRotationRoll(roll);

        drive.setColor(colorCode);
        drive.setIsCritical(critical);
        drive.setKnockBack(knockback);

        drive.setLifetime(lifetime);

        drive.setRank(7.0f);

        mobIn.level().addFreshEntity(drive);


        return drive;
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        Entity targetEntity = entityHitResult.getEntity();
        float damageValue = (float) this.getDamage();
        Entity shooter = this.getShooter();
        DamageSource damagesource;
        if (shooter == null) {
            damagesource = this.damageSources().indirectMagic(this, this);
        } else {
            damagesource = this.damageSources().indirectMagic(this, shooter);
            if (shooter instanceof LivingEntity living) {
                Entity hits = targetEntity;
                if (targetEntity instanceof PartEntity<?> part) {
                    hits = part.getParent();
                }

                living.setLastHurtMob(hits);
            }
        }

        int fireTime = targetEntity.getRemainingFireTicks();
        if (this.isOnFire() && !(targetEntity instanceof EnderMan)) {
            targetEntity.setSecondsOnFire(5);
        }

        targetEntity.invulnerableTime = 0;
        if (this.getOwner() instanceof LivingEntity living) {
            damageValue = (float) (damageValue * living.getAttributeValue(Attributes.ATTACK_DAMAGE));
            float rankDamageBonus = 3.5f;

            damageValue += rankDamageBonus;

            damageValue = (float) ((double) damageValue * (double) AttackManager.getSlashBladeDamageScale(living));
            if (this.getIsCritical()) {
                damageValue += (float) this.random.nextInt(Mth.ceil(damageValue) / 2 + 2);
            }
        }
//        System.out.println(((LivingEntity)targetEntity).getHealth());
        if (targetEntity.hurt(damagesource, damageValue)) {
            Entity hits = targetEntity;
            if (targetEntity instanceof PartEntity<?> part) {
                hits = part.getParent();
            }

            if (hits instanceof LivingEntity targetLivingEntity) {
                StunManager.setStun(targetLivingEntity);
                if (!this.level().isClientSide() &&
                        shooter instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(targetLivingEntity, shooter);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity) shooter, targetLivingEntity);
                }

                this.affectEntity(targetLivingEntity, this.getPotionEffects(), 1.0);
                if (targetLivingEntity != shooter && targetLivingEntity instanceof Player && shooter instanceof ServerPlayer) {
                    ((ServerPlayer) shooter).playNotifySound(this.getHitEntityPlayerSound(), SoundSource.PLAYERS, 0.18F, 0.45F);
                }
            }

            this.playSound(this.getHitEntitySound(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        } else {
            targetEntity.setRemainingFireTicks(fireTime);
        }

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
        if (this.getHitEntity() != null) {
            Entity hits = this.getHitEntity();
            if (!hits.isAlive()) {
                this.burst();
            } else {
                this.setPos(hits.getX(), hits.getY() + (double) (hits.getEyeHeight() * 0.5F), hits.getZ());
                int delay = this.getDelay();
                --delay;
                this.setDelay(delay);
                if (!this.level().isClientSide() && delay < 0) {
                    this.burst();
                }
            }

        } else {
            boolean disallowedHitBlock = this.isNoClip();
            BlockPos blockpos = this.getOnPos();
            BlockState blockstate = this.level().getBlockState(blockpos);
            EntityAbstractSummonedSwordAccessor accessor = (EntityAbstractSummonedSwordAccessor) this;

            if (!blockstate.isAir() && !disallowedHitBlock) {
                VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
                if (!voxelshape.isEmpty()) {
                    Iterator var5 = voxelshape.toAabbs().iterator();

                    while (var5.hasNext()) {
                        AABB axisalignedbb = (AABB) var5.next();
                        if (axisalignedbb.move(blockpos).contains(new Vec3(this.getX(), this.getY(), this.getZ()))) {
                            accessor.setInGround(true);
                            break;
                        }
                    }
                }
            }

            if (this.isInWaterOrRain()) {
                this.clearFire();
            }

            if (accessor.getInGround() && !disallowedHitBlock) {
                if (accessor.getInBlockState() != blockstate && this.level().noCollision(this.getBoundingBox().inflate(0.06))) {
                    this.burst();
                } else if (!this.level().isClientSide()) {
                    this.tryDespawn();
                }
            } else {
                Vec3 motionVec = this.getDeltaMovement();
                if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
                    float f = Mth.sqrt((float) motionVec.horizontalDistanceSqr());
                    this.setYRot((float) (Mth.atan2(motionVec.x, motionVec.z) * 57.2957763671875));
                    this.setXRot((float) (Mth.atan2(motionVec.y, f) * 57.2957763671875));
                    this.yRotO = this.getYRot();
                    this.xRotO = this.getXRot();
                }

                accessor.setTicksInAir(accessor.getTicksInAir() + 1);
                Vec3 positionVec = this.position();
                Vec3 movedVec = positionVec.add(motionVec);
                HitResult raytraceresult = this.level().clip(new ClipContext(positionVec, movedVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (raytraceresult.getType() != HitResult.Type.MISS) {
                    movedVec = raytraceresult.getLocation();
                }

                while (this.isAlive()) {
                    EntityHitResult entityraytraceresult = this.getRayTrace(positionVec, movedVec);
                    if (entityraytraceresult != null) {
                        raytraceresult = entityraytraceresult;
                    }

                    if (raytraceresult != null && raytraceresult.getType() == HitResult.Type.ENTITY) {
                        Entity entity = null;
                        if (raytraceresult instanceof EntityHitResult result) {
                            entity = result.getEntity();
                        }
                        Entity entity1 = this.getShooter();
                        if (entity instanceof LivingEntity target &&
                                entity1 instanceof LivingEntity attacker &&
                                !MobTargetSelector.test.test(attacker, target)) {
                            raytraceresult = null;
                            entityraytraceresult = null;
                        }
                    }

                    if (raytraceresult != null && (!disallowedHitBlock || raytraceresult.getType() != HitResult.Type.BLOCK) && !ForgeEventFactory.onProjectileImpact(this, raytraceresult)) {
                        this.onHit(raytraceresult);
                        this.hasImpulse = true;
                    }

                    if (entityraytraceresult == null || this.getPierce() <= 0) {
                        break;
                    }

                    raytraceresult = null;
                }

                motionVec = this.getDeltaMovement();
                double mx = motionVec.x;
                double my = motionVec.y;
                double mz = motionVec.z;
                if (this.getIsCritical()) {
                    for (int i = 0; i < 4; ++i) {
                        this.level().addParticle(ParticleTypes.CRIT, this.getX() + mx * (double) i / 4.0, this.getY() + my * (double) i / 4.0, this.getZ() + mz * (double) i / 4.0, -mx, -my + 0.2, -mz);
                    }
                }

                this.setPos(this.getX() + mx, this.getY() + my, this.getZ() + mz);
                float f4 = Mth.sqrt((float) motionVec.horizontalDistanceSqr());
                if (disallowedHitBlock) {
                    this.setYRot((float) (Mth.atan2(-mx, -mz) * 57.2957763671875));
                } else {
                    this.setYRot((float) (Mth.atan2(mx, mz) * 57.2957763671875));
                }

                this.setXRot((float) (Mth.atan2(my, f4) * 57.2957763671875));

                while (this.getXRot() - this.xRotO < -180.0F) {
                    this.xRotO -= 360.0F;
                }

                while (this.getXRot() - this.xRotO >= 180.0F) {
                    this.xRotO += 360.0F;
                }

                while (this.getYRot() - this.yRotO < -180.0F) {
                    this.yRotO -= 360.0F;
                }

                while (this.getYRot() - this.yRotO >= 180.0F) {
                    this.yRotO += 360.0F;
                }

                this.setXRot(Mth.lerp(0.2F, this.xRotO, this.getXRot()));
                this.setYRot(Mth.lerp(0.2F, this.yRotO, this.getYRot()));
                float f1 = 0.99F;
                if (this.isInWater()) {
                    for (int j = 0; j < 4; ++j) {
                        this.level().addParticle(ParticleTypes.BUBBLE, this.getX() - mx * 0.25, this.getY() - my * 0.25, this.getZ() - mz * 0.25, mx, my, mz);
                    }
                }

                this.setDeltaMovement(motionVec.scale(f1));
                if (!this.isNoGravity() && !disallowedHitBlock) {
                    Vec3 vec3d3 = this.getDeltaMovement();
                    this.setDeltaMovement(vec3d3.x, vec3d3.y - 0.05000000074505806, vec3d3.z);
                }

                this.checkInsideBlocks();
            }

            if (!this.level().isClientSide() && accessor.getTicksInGround() <= 0 && 100 < this.tickCount) {
                this.remove(RemovalReason.DISCARDED);
            }

        }
    }

    @Override
    public void burst(List<MobEffectInstance> effects, @Nullable Entity focusEntity) {
        List<Entity> list = MobTargetSelector.getTargettableEntitiesWithinAABB(this.level(), 2.0, this);
        list.stream()
                .filter((e) -> e instanceof LivingEntity)
                .map((e) -> (LivingEntity) e).forEach((e) -> {
                    double distanceSq = this.distanceToSqr(e);
                    if (distanceSq < 9.0) {
                        double factor = 1.0 - Math.sqrt(distanceSq) / 4.0;
                        if (e == focusEntity) {
                            factor = 1.0;
                        }

                        this.affectEntity(e, effects, factor);
                    }

                });
    }

}
