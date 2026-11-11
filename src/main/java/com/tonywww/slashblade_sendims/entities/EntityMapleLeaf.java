package com.tonywww.slashblade_sendims.entities;

import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 天云 SE「鸦」放出的红枫叶：先短暂飘散，随后锁定离自己最近的敌人追踪，命中造成一次魔法伤害后消失。
 * 只存在于运行时，不写入存档。
 */
public final class EntityMapleLeaf extends Entity {
    public static final int LAUNCH_TICKS = 5;
    public static final int MAX_LIFE = 80;
    public static final double SEARCH_RADIUS = 16.0D;
    public static final double MAX_SPEED = 1.1D;

    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.88F, 0.12F, 0.10F), 0.7F);

    @Nullable private UUID ownerId;
    @Nullable private LivingEntity cachedOwner;
    private float damage;
    private int targetId = -1;

    public EntityMapleLeaf(EntityType<? extends EntityMapleLeaf> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public void configure(LivingEntity owner, Vec3 position, Vec3 velocity, float damage) {
        this.ownerId = owner.getUUID();
        this.cachedOwner = owner;
        this.damage = damage;
        setPos(position);
        setDeltaMovement(velocity);
    }

    public float damage() { return damage; }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (tickCount % 2 == 0) {
                level().addParticle(RED, getX(), getY() + 0.1D, getZ(), 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        if (tickCount > MAX_LIFE) { discard(); return; }
        LivingEntity owner = owner();
        if (owner == null || !owner.isAlive()) { discard(); return; }

        Vec3 velocity = getDeltaMovement();
        if (tickCount <= LAUNCH_TICKS) {
            velocity = velocity.scale(0.82D).add(0.0D, -0.012D, 0.0D);
        } else {
            LivingEntity target = target();
            if (target == null) target = acquire(owner);
            if (target == null) {
                velocity = velocity.scale(0.9D).add(0.0D, -0.015D, 0.0D);
            } else {
                Vec3 aim = target.getBoundingBox().getCenter().subtract(position());
                double distance = aim.length();
                double speed = Math.min(MAX_SPEED, 0.35D + (tickCount - LAUNCH_TICKS) * 0.08D);
                if (distance <= speed + target.getBbWidth() * 0.5D + 0.35D
                        || getBoundingBox().inflate(0.15D).intersects(target.getBoundingBox())) {
                    strike(owner, target);
                    return;
                }
                velocity = velocity.lerp(aim.scale(speed / Math.max(distance, 1.0E-4D)), 0.45D);
            }
        }
        setDeltaMovement(velocity);
        setPos(position().add(velocity));
    }

    @Nullable
    private LivingEntity owner() {
        if (cachedOwner == null || cachedOwner.isRemoved()) {
            cachedOwner = null;
            if (ownerId == null || !(level() instanceof ServerLevel server)) return null;
            if (server.getEntity(ownerId) instanceof LivingEntity living) cachedOwner = living;
        }
        return cachedOwner != null && cachedOwner.level() == level() ? cachedOwner : null;
    }

    @Nullable
    private LivingEntity target() {
        if (targetId < 0) return null;
        Entity entity = level().getEntity(targetId);
        if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) return living;
        targetId = -1;
        return null;
    }

    @Nullable
    private LivingEntity acquire(LivingEntity owner) {
        AABB area = getBoundingBox().inflate(SEARCH_RADIUS);
        LivingEntity nearest = null;
        double best = SEARCH_RADIUS * SEARCH_RADIUS;
        // The 3-argument overload limits targets to the owner's melee reach around the owner;
        // a leaf flies from wherever the slash landed, so allow anything the leaf itself can reach.
        double reach = owner.position().distanceTo(position()) + SEARCH_RADIUS + 2.0D;
        for (Entity candidate : TargetSelector.getTargettableEntitiesWithinAABB(level(), owner, area, reach)) {
            if (!(candidate instanceof LivingEntity living) || living == owner || !living.isAlive()) continue;
            double d = living.getBoundingBox().getCenter().distanceToSqr(position());
            if (d < best) { best = d; nearest = living; }
        }
        targetId = nearest == null ? -1 : nearest.getId();
        return nearest;
    }

    private void strike(LivingEntity owner, LivingEntity target) {
        if (damage > 0.0F) {
            target.invulnerableTime = 0;
            target.hurt(damageSources().indirectMagic(this, owner), damage);
        }
        if (level() instanceof ServerLevel server) {
            Vec3 c = target.getBoundingBox().getCenter();
            server.sendParticles(RED, c.x, c.y, c.z, 12, 0.25D, 0.25D, 0.25D, 0.02D);
            server.playSound(null, c.x, c.y, c.z, SoundEvents.CHERRY_LEAVES_BREAK, SoundSource.PLAYERS, 0.6F, 1.3F);
        }
        discard();
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
