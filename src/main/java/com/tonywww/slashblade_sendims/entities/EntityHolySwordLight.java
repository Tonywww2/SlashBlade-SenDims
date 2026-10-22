package com.tonywww.slashblade_sendims.entities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** A synchronized, ownerless visual. It never attacks, collides or breaks blocks. */
public final class EntityHolySwordLight extends Entity {
    private static final EntityDataAccessor<Float> DX = SynchedEntityData.defineId(EntityHolySwordLight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DY = SynchedEntityData.defineId(EntityHolySwordLight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DZ = SynchedEntityData.defineId(EntityHolySwordLight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(EntityHolySwordLight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(EntityHolySwordLight.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(EntityHolySwordLight.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WAVE = SynchedEntityData.defineId(EntityHolySwordLight.class, EntityDataSerializers.BOOLEAN);

    public EntityHolySwordLight(EntityType<? extends EntityHolySwordLight> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
    }

    @Override protected void defineSynchedData() {
        entityData.define(DX, 0.0F); entityData.define(DY, 0.0F); entityData.define(DZ, 1.0F);
        entityData.define(LENGTH, 1.0F); entityData.define(WIDTH, 1.0F);
        entityData.define(LIFETIME, 12); entityData.define(WAVE, false);
    }

    public void configure(Vec3 start, Vec3 end, float width, int lifetime, boolean wave) {
        setPos(start);
        Vec3 direction = end.subtract(start).normalize();
        entityData.set(DX, (float) direction.x); entityData.set(DY, (float) direction.y); entityData.set(DZ, (float) direction.z);
        entityData.set(LENGTH, (float) start.distanceTo(end)); entityData.set(WIDTH, width);
        entityData.set(LIFETIME, lifetime); entityData.set(WAVE, wave);
    }

    public Vec3 direction() { return new Vec3(entityData.get(DX), entityData.get(DY), entityData.get(DZ)); }
    public float length() { return entityData.get(LENGTH); }
    public float width() { return entityData.get(WIDTH); }
    public int lifetime() { return entityData.get(LIFETIME); }
    public boolean wave() { return entityData.get(WAVE); }
    @Override public void tick() { super.tick(); if (tickCount >= lifetime()) discard(); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
