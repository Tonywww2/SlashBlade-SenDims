package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.entities.EntityChaoticJudgementCut;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.RayTraceHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

    public static EntityChaoticJudgementCut doJudgementCutJust(LivingEntity user) {
        EntityChaoticJudgementCut judgementCut = doJudgementCut(user);
        judgementCut.setIsCritical(true);
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
                SlashBlade.RegistryEvents.JudgementCut,
                level
        );
        judgementCut.setPos(position.x, position.y, position.z);
        judgementCut.setOwner(owner);
        owner.getMainHandItem()
                .getCapability(ItemSlashBlade.BLADESTATE)
                .ifPresent(state -> judgementCut.setColor(state.getColorCode()));
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
}