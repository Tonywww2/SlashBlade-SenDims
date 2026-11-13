package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.projectile.LichBolt;
import twilightforest.entity.projectile.LichBomb;
import twilightforest.init.TFSounds;

@Mixin(value = Lich.class, remap = true)
public abstract class LichMixin extends Monster {

    @Final
    @Shadow(remap = false)
    public static int INITIAL_SHIELD_STRENGTH = 8;

    @Unique
    private static String SHIELD_BREAK_CD_PATH = "sbsd.lich.shield_cd";

    @Unique
    private static int SHIELD_BREAK_CD = 80;

    @Shadow(remap = false)
    public abstract int getShieldStrength();

    @Shadow(remap = false)
    public abstract int getPhase();

    protected LichMixin(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    // 只替换护盾初值，不取消原方法，其他模组在 defineSynchedData 里定义的同步数据才能保留。
    @ModifyArg(
            method = "defineSynchedData()V",
            slice = @Slice(from = @At(
                    value = "FIELD",
                    target = "Ltwilightforest/entity/boss/Lich;SHIELD_STRENGTH:Lnet/minecraft/network/syncher/EntityDataAccessor;",
                    opcode = Opcodes.GETSTATIC,
                    remap = false)),
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/syncher/SynchedEntityData;define(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V",
                    ordinal = 0),
            index = 1)
    private Object modifyInitialShieldStrength(Object value) {
        return INITIAL_SHIELD_STRENGTH;
    }

    @Redirect(
            method = "aiStep()V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerBossEvent;setProgress(F)V")
    )
    private void redirectAiStep(ServerBossEvent instance, float pProgress) {
        if (getPhase() == 1) {
            instance.setProgress((float) getShieldStrength() / INITIAL_SHIELD_STRENGTH);

        } else {
            instance.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    @Inject(method = "launchProjectileAt(Lnet/minecraft/world/entity/projectile/ThrowableProjectile;)V", at = @At("RETURN"), remap = false)
    private void injectLaunchProjectileAt(ThrowableProjectile projectile, CallbackInfo ci) {
        if (projectile instanceof LichBolt bolt && bolt.getOwner() instanceof Lich lich) {
            if (!lich.isShadowClone()) {
                SBSDLeader.doLeaderSAMagicSLash(lich, null);

            }

        } else if (projectile instanceof LichBomb bomb && bomb.getOwner() instanceof Lich lich) {
            if (!lich.isShadowClone()) {
                SBSDLeader.doLeaderSAMagicDrive(lich, null);

            }
        }

    }

    @Inject(method = "setShieldStrength(I)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectSetShieldStrength(int shieldStrength, CallbackInfo ci) {
        if (shieldStrength < getShieldStrength()) {
            if (!this.getPersistentData().contains(SHIELD_BREAK_CD_PATH) ||
                    this.getPersistentData().getInt(SHIELD_BREAK_CD_PATH) <= 0) {
                this.getPersistentData().putInt(SHIELD_BREAK_CD_PATH, SHIELD_BREAK_CD);

            } else {
                this.getPersistentData().putInt(SHIELD_BREAK_CD_PATH, this.getPersistentData().getInt(SHIELD_BREAK_CD_PATH) - 2);
                this.playSound(TFSounds.SHIELD_BREAK.get(), 0.8F, 0.5F);
                ci.cancel();
            }

        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getPersistentData().contains(SHIELD_BREAK_CD_PATH) && this.getPersistentData().getInt(SHIELD_BREAK_CD_PATH) > 0) {
            this.getPersistentData().putInt(SHIELD_BREAK_CD_PATH, this.getPersistentData().getInt(SHIELD_BREAK_CD_PATH) - 1);

        }
    }
}
