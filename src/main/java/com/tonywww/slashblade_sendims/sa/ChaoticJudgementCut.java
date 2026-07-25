package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.entities.EntityChaoticJudgementCut;
import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import com.tonywww.slashblade_sendims.utils.ChaoticSlashArtEffects;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AdvancementHelper;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ChaoticJudgementCut {
    private ChaoticJudgementCut() {
    }

    public static void doCharge(LivingEntity user) {
        long elapsed = ComboState.getElapsed(user);
        if (elapsed == 0L) {
            user.playSound(
                    SoundEvents.TRIDENT_THROW,
                    0.8F,
                    0.625F + 0.1F * user.getRandom().nextFloat()
            );
            AdvancementHelper.grantCriterion(user, AdvancementHelper.ADVANCEMENT_JUDGEMENT_CUT);
        }

        if (elapsed <= 3L) {
            user.moveRelative(-0.3F, new Vec3(0.0D, 0.0D, 1.0D));
            Vec3 movement = user.getDeltaMovement();
            double x = movement.x;
            double z = movement.z;

            while (x != 0.0D && user.level().noCollision(
                    user,
                    user.getBoundingBox().move(x, -user.maxUpStep(), 0.0D)
            )) {
                x = approachZero(x);
            }
            while (z != 0.0D && user.level().noCollision(
                    user,
                    user.getBoundingBox().move(0.0D, -user.maxUpStep(), z)
            )) {
                z = approachZero(z);
            }
            while (x != 0.0D && z != 0.0D && user.level().noCollision(
                    user,
                    user.getBoundingBox().move(x, -user.maxUpStep(), z)
            )) {
                x = approachZero(x);
                z = approachZero(z);
            }

            user.move(MoverType.SELF, new Vec3(x, movement.y, z));
        }

        user.setDeltaMovement(user.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
    }

    public static void doJudgementCutAir(LivingEntity user) {
        doJudgementCut(user);
        AdvancementHelper.grantCriterion(user, AdvancementHelper.ADVANCEMENT_JUDGEMENT_CUT);
    }

    public static EntityChaoticJudgementCut doJudgementCutJust(LivingEntity user) {
        EntityChaoticJudgementCut judgementCut = doJudgementCut(user);
        judgementCut.setIsCritical(true);
        AdvancementHelper.grantCriterion(user, AdvancementHelper.ADVANCEMENT_JUDGEMENT_CUT_JUST);
        return judgementCut;
    }

    public static EntityChaoticJudgementCut doJudgementCut(LivingEntity user) {
        Level level = user.level();
        Vec3 eyePos = user.getEyePosition(1.0F);
        ItemStack stack = user.getMainHandItem();
        Optional<Vec3> targetPos = stack.getCapability(ItemSlashBlade.BLADESTATE)
                .filter(state -> state.getTargetEntity(level) != null)
                .map(state -> Objects.requireNonNull(state.getTargetEntity(level)).getEyePosition(1.0F));

        if (targetPos.isEmpty()) {
            targetPos = RayTraceHelper.rayTrace(
                    level,
                    user,
                    eyePos,
                    user.getLookAngle(),
                    5.0D,
                    7.0D,
                    entity -> !entity.isSpectator()
                            && entity.isAlive()
                            && entity.isPickable()
                            && entity != user
            ).map(ChaoticJudgementCut::resolveHitPosition);
        }

        Vec3 position = targetPos.orElseGet(() -> eyePos.add(user.getLookAngle().scale(5.0D)));
        EntityChaoticJudgementCut judgementCut = create(user, position);
        level.playSound(
                null,
                judgementCut.getX(),
                judgementCut.getY(),
                judgementCut.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.5F,
                0.8F / (user.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        return judgementCut;
    }

    public static void doJudgementCutSuper(LivingEntity owner) {
        doJudgementCutSuper(owner, null);
    }

    public static void doJudgementCutSuper(LivingEntity owner, List<Entity> excluded) {
        Level level = owner.level();
        List<Entity> targets = TargetSelector.getTargettableEntitiesWithinAABB(
                level,
                owner,
                owner.getBoundingBox().inflate(48.0D),
                TargetSelector.getResolvedReach(owner) + 32.0D
        );
        if (excluded != null) {
            targets.removeAll(excluded);
        }

        for (Entity target : targets) {
            if (target instanceof LivingEntity livingTarget) {
                livingTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10));
                create(owner, target.position());
            }
        }

        level.playSound(
                owner,
                owner.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                1.0F,
                1.0F
        );
    }

    private static EntityChaoticJudgementCut create(LivingEntity owner, Vec3 position) {
        Level level = owner.level();
        EntityChaoticJudgementCut judgementCut = new EntityChaoticJudgementCut(
            SBSDEntities.CHAOTIC_JUDGEMENT_CUT.get(),
                level
        );
        judgementCut.setPos(position.x, position.y, position.z);
        judgementCut.setOwner(owner);
        judgementCut.setColor(ChaoticSlashArtEffects.PRIMARY_COLOR);
        owner.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                .ifPresent(rank -> judgementCut.setRank(rank.getRankLevel(level.getGameTime())));
        level.addFreshEntity(judgementCut);
        return judgementCut;
    }

    private static Vec3 resolveHitPosition(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            return target.position().add(0.0D, target.getEyeHeight() / 2.0D, 0.0D);
        }
        return hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getLocation() : null;
    }

    private static double approachZero(double value) {
        if (value < 0.05D && value >= -0.05D) {
            return 0.0D;
        }
        return value > 0.0D ? value - 0.05D : value + 0.05D;
    }
}