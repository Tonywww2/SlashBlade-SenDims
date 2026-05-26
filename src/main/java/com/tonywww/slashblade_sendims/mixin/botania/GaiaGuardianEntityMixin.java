package com.tonywww.slashblade_sendims.mixin.botania;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.botania.common.entity.GaiaGuardianEntity;
import vazkii.botania.common.helper.PlayerHelper;
import vazkii.botania.common.helper.VecHelper;

import java.util.List;
import java.util.UUID;

@Mixin(value = GaiaGuardianEntity.class)
public abstract class GaiaGuardianEntityMixin extends Mob {

    @Shadow(remap = false)
    private int tpDelay;

    @Shadow(remap = false)
    private boolean spawnPixies;

    @Shadow(remap = false)
    @Final
    private List<UUID> playersWhoAttacked;

    @Shadow(remap = false)
    public abstract int getInvulTime();

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true, remap = true)
    private void injectHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = source.getEntity();
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            if (PlayerHelper.isTruePlayer(entity)) {
                if (this.getInvulTime() == 0) {
                    if (!this.playersWhoAttacked.contains(player.getUUID())) {
                        this.playersWhoAttacked.add(player.getUUID());
                    }
                    cir.setReturnValue(super.hurt(source, amount));
                    return;
                }
            }
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F", at = @At("HEAD"), cancellable = true, remap = true)
    private void injectGetDamageAfterArmorAbsorb(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(super.getDamageAfterArmorAbsorb(source, amount));
    }

    protected GaiaGuardianEntityMixin(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), cancellable = true, remap = true)
    private void injectActuallyHurt(DamageSource source, float amount, CallbackInfo ci) {
        if (!this.isInvulnerableTo(source)) {
            amount = net.minecraftforge.common.ForgeHooks.onLivingHurt(this, source, amount);
            if (amount <= 0) return;
            amount = this.getDamageAfterArmorAbsorb(source, amount);
            amount = this.getDamageAfterMagicAbsorb(source, amount);
            float f1 = Math.max(amount - this.getAbsorptionAmount(), 0.0F);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - (amount - f1));
            float f = amount - f1;
            if (f > 0.0F && f < 3.4028235E37F) {
                Entity entity = source.getEntity();
                if (entity instanceof ServerPlayer serverplayer) {
                    serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0F));
                }
            }

            f1 = net.minecraftforge.common.ForgeHooks.onLivingDamage(this, source, f1);
            if (f1 != 0.0F) {
                this.getCombatTracker().recordDamage(source, f1);
                this.setHealth(this.getHealth() - f1);
                this.setAbsorptionAmount(this.getAbsorptionAmount() - f1);
                this.gameEvent(GameEvent.ENTITY_DAMAGE);
            }
        }

        Entity attacker = source.getDirectEntity();
        if (attacker != null) {
            Vec3 thisVector = VecHelper.fromEntityCenter(this);
            Vec3 playerVector = VecHelper.fromEntityCenter(attacker);
            Vec3 motionVector = thisVector.subtract(playerVector).normalize().scale(0.75d);
            if (this.getHealth() > 0.0F) {
                this.setDeltaMovement(-motionVector.x, 0.5F, -motionVector.z);
                this.tpDelay = 4;
                this.spawnPixies = true;
            }
        }

        this.invulnerableTime = Math.max(this.invulnerableTime, 20);
        ci.cancel();
    }

}
