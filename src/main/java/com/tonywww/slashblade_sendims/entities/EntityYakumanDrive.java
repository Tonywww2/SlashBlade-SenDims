package com.tonywww.slashblade_sendims.entities;

import mods.flammpfeil.slashblade.entity.EntityDrive;
import mods.flammpfeil.slashblade.entity.Projectile;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

/** Native phantom-blade movement and renderer; the cast owns swept hits and their effects. */
public final class EntityYakumanDrive extends EntityDrive {
    // Bounds of the shared SlashBlade 1.9.65 drive mesh, after the native 0.015 render scale.
    public static final float MODEL_WIDTH=194.014602F*.015F, MODEL_HEIGHT=14.8064F*.015F,
            MODEL_DEPTH=97.857399F*.015F, MODEL_CENTER_DEPTH=81.0713005F*.015F;
    private static final EntityDataAccessor<Float> HEIGHT_SCALE=
            SynchedEntityData.defineId(EntityYakumanDrive.class,EntityDataSerializers.FLOAT);
    private Vec3 correction=Vec3.ZERO;
    private int correctionTicks;

    public EntityYakumanDrive(EntityType<? extends Projectile> type,Level level) {
        super(type,level);
        setNoClip(true);setNoGravity(true);
    }

    @Override protected void defineSynchedData() {
        super.defineSynchedData();getEntityData().define(HEIGHT_SCALE,1F);
    }
    public void setVisualDimensions(float width,float height) {
        float size=width/MODEL_WIDTH;setBaseSize(size);
        getEntityData().set(HEIGHT_SCALE,height/(MODEL_HEIGHT*size));
    }
    public float getHeightScale() {return getEntityData().get(HEIGHT_SCALE);}
    @Override public AABB getBoundingBoxForCulling() {
        double radius=getBaseSize()*Math.max(MODEL_WIDTH,MODEL_HEIGHT*getHeightScale())*.5;
        return getBoundingBox().inflate(radius);
    }

    @Override public void tick() {
        Vec3 velocity=getDeltaMovement();
        float previousYaw=getYRot(),previousPitch=getXRot();
        super.tick();
        // Keep the SA's configured speed instead of the native arrow drag.
        setDeltaMovement(velocity);
        // Native no-clip movement reverses yaw by 180 degrees. Restore cutting-edge-first flight.
        if(velocity.lengthSqr()>1E-8) {
            float yaw=(float)(Mth.atan2(velocity.x,velocity.z)*180/Math.PI);
            float pitch=(float)(Mth.atan2(velocity.y,Math.sqrt(velocity.horizontalDistanceSqr()))*180/Math.PI);
            setYRot(yaw);setXRot(pitch);
            yRotO=previousYaw;xRotO=previousPitch;
        }
        if(level().isClientSide()&&correctionTicks>0) {
            Vec3 step=correction.scale(1D/correctionTicks);
            setPos(position().add(step));
            correction=correction.subtract(step);correctionTicks--;
        }
    }

    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport) {
        if(!level().isClientSide()) {super.lerpTo(x,y,z,yaw,pitch,steps,teleport);return;}
        // Reconcile tracking packets over several frames without resetting the flight position.
        correction=new Vec3(x,y,z).subtract(position());
        correctionTicks=Math.max(1,Math.min(3,steps));
    }

    // Avoid a second collision scan and the native damage path: YakumanArts handles each hit once.
    @Override public EntityHitResult getRayTrace(Vec3 start,Vec3 end) {return null;}
    @Override protected void onHitEntity(EntityHitResult hit) {}
    @Override protected void onHitBlock(BlockHitResult hit) {}
}
