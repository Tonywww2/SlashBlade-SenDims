package com.tonywww.slashblade_sendims.mixin.twilightforest;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.entity.boss.Lich;
import twilightforest.entity.projectile.LichBolt;
import twilightforest.entity.projectile.LichBomb;
import twilightforest.init.TFSounds;

import java.util.Optional;

@Mixin(value = Lich.class, remap = false)
public abstract class LichMixin extends Monster {

    @Final
    @Shadow
    private static EntityDataAccessor<Boolean> IS_CLONE;

    @Final
    @Shadow
    private static EntityDataAccessor<Integer> SHIELD_STRENGTH;

    @Final
    @Shadow
    private static EntityDataAccessor<Integer> MINIONS_LEFT;

    @Final
    @Shadow
    private static EntityDataAccessor<Integer> ATTACK_TYPE;

    @Final
    @Shadow
    private static EntityDataAccessor<Optional<GlobalPos>> HOME_POINT;

    @Final
    @Shadow
    public static int INITIAL_SHIELD_STRENGTH = 12;

    @Unique
    private static String SHIELD_BREAK_CD_PATH = "sbsd.lich.shield_cd";

    @Unique
    private static int SHIELD_BREAK_CD = 120;

    @Shadow
    public abstract int getShieldStrength();

    @Shadow
    public abstract int getPhase();

    protected LichMixin(EntityType<? extends Monster> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "defineSynchedData()V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectDefineSynchedData(CallbackInfo ci) {
        super.defineSynchedData();
        this.getEntityData().define(IS_CLONE, false);
        this.getEntityData().define(SHIELD_STRENGTH, INITIAL_SHIELD_STRENGTH);
        this.getEntityData().define(MINIONS_LEFT, Lich.INITIAL_MINIONS_TO_SUMMON);
        this.getEntityData().define(ATTACK_TYPE, 0);
        this.getEntityData().define(HOME_POINT, Optional.empty());
        ci.cancel();
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
            SBSDLeader.doLeaderSAMagicSLash(lich, null);

        } else if (projectile instanceof LichBomb bomb && bomb.getOwner() instanceof Lich lich) {
            SBSDLeader.doLeaderSAMagicDrive(lich, null);
        }

    }

    @Inject(method = "setShieldStrength(I)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void injectSetShieldStrength(int shieldStrength, CallbackInfo ci) {
        if (shieldStrength < getShieldStrength()) {
            if (!this.getPersistentData().contains(SHIELD_BREAK_CD_PATH) ||
                    this.getPersistentData().getInt(SHIELD_BREAK_CD_PATH) <= 0) {
                this.getPersistentData().putInt(SHIELD_BREAK_CD_PATH, SHIELD_BREAK_CD);

            } else {
                this.getPersistentData().putInt(SHIELD_BREAK_CD_PATH, this.getPersistentData().getInt(SHIELD_BREAK_CD_PATH) - 1);
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
