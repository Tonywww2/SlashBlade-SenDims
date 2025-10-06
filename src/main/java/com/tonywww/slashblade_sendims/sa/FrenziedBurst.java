package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.se.FrenziedFlame;
import com.tonywww.slashblade_sendims.utils.SlashBladeUtil;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FrenziedBurst {
    public static void doFrenziedBurst(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;

        ItemStack bladeStack = attacker.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;

        ISlashBladeState state = SlashBladeUtil.getState(bladeStack);
        Entity target = state.getTargetEntity(serverLevel);
        if (target == null) {
            Vec3 startVec = attacker.getEyePosition();
            Vec3 lookVec = attacker.getLookAngle().scale(64.0d);
            Vec3 endVec = startVec.add(lookVec);
            EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                    attacker.level(),
                    attacker,
                    startVec,
                    endVec,
                    attacker.getBoundingBox().expandTowards(lookVec).inflate(1.0d),
                    e -> !e.isSpectator() && e.isPickable()
            );

            target = entityHitResult != null ? entityHitResult.getEntity() : null;
            if (target == null) return;
        }

        Vec3 startPos = attacker.position().add(attacker.getBbWidth() / 2, attacker.getBbHeight(), attacker.getBbWidth() / 2);
        Vec3 endPos = target.position().add(target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2);

        boolean hasFrenziedFlame = state.hasSpecialEffect(SBSDSpecialEffects.FRENZIED_FLAME.getId());

        if (hasFrenziedFlame) {
            if (target instanceof LivingEntity livingEntity) {
                drawFrenziedParticleCurve(serverLevel, startPos, endPos, 192);

                AttributeInstance instanceFrenzyDamage = attacker.getAttribute(SBSDAttributes.FRENZY_DAMAGE.get());
                float damageScale = instanceFrenzyDamage == null ?
                        1f :
                        (float) (1d + instanceFrenzyDamage.getValue());

                AttributeInstance instanceFrenzyResistance = livingEntity.getAttribute(SBSDAttributes.FRENZY_RESISTANCE.get());
                damageScale = instanceFrenzyResistance == null ?
                        damageScale :
                        (float) (damageScale * (1d - instanceFrenzyResistance.getValue()));

                AttackManager.doMeleeAttack(attacker, target, true, true,
                        2.0f * damageScale);
                AttackManager.doMeleeAttack(attacker, target, true, true,
                        2.0f * damageScale);
                int expLevel = attacker instanceof Player player ?
                        player.experienceLevel :
                        30;
                int finalMadness = FrenziedFlame.getFinalMadness(attacker, expLevel * 3) * 2;

                FrenziedFlame.addMadness(livingEntity, attacker, finalMadness);
                FrenziedFlame.addMadness(attacker, attacker, FrenziedFlame.BASE_MADNESS * 3);

            }

        } else {
            drawFrenziedParticleCurve(serverLevel, startPos, endPos, 64);
            AttackManager.doMeleeAttack(attacker, target, true, true,
                    2.0f);

            FrenziedFlame.addMadness(attacker, attacker, FrenziedFlame.BASE_MADNESS * 2);

        }


    }

    public static void doPreFrenziedBurst(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;

        double x = attacker.getX();
        double y = attacker.getY() + attacker.getBbHeight();
        double z = attacker.getZ();

        serverLevel.sendParticles(
                FrenziedFlame.FRENZY_PARTICLE_1,
                x, y, z,
                3,
                0.05d, 0.05d, 0.05d,
                0d
        );

        serverLevel.sendParticles(
                FrenziedFlame.FRENZY_PARTICLE_2,
                x, y, z,
                9,
                0.15d, 0.15d, 0.15d,
                0d
        );

    }

    public static void drawFrenziedParticleCurve(ServerLevel level, Vec3 start, Vec3 end, int baseDensity) {
        double startX = start.x();
        double startY = start.y();
        double startZ = start.z();

        // 中点
        double midX = (startX + end.x()) / 2.0d;
        double midY = (startY + end.y()) / 2.0d;
        double midZ = (startZ + end.z()) / 2.0d;

        // 计算起点到终点的距离
        double distance = Math.sqrt(
                Math.pow(end.x() - startX, 2d) +
                        Math.pow(end.y() - startY, 2d) +
                        Math.pow(end.z() - startZ, 2d)
        );

        // 距离越远粒子越多
        double distanceFactor = Math.min(distance / 10.0d, 1.0d);

        int adjustedDensity = (int) (baseDensity * (0.3d + 0.7d * distanceFactor));

        // 距离越近,弯曲度越高
        double curvatureFactor = Math.max(2.0d - distanceFactor * 1.8d, 0.1d);
        double maxOffset = distance * curvatureFactor;
        double offsetX = (level.getRandom().nextDouble() - 0.5d) * maxOffset;
        double offsetY = (level.getRandom().nextDouble() - 0.2d) * maxOffset * 0.8d;
        double offsetZ = (level.getRandom().nextDouble() - 0.5d) * maxOffset;

        // 控制点
        double controlX = midX + offsetX;
        double controlY = midY + offsetY;
        double controlZ = midZ + offsetZ;

        double step = 1.0d / adjustedDensity;

        for (double t = 0d; t <= 1d; t += step) {
            double oneMinusT = 1d - t;
            double oneMinusTSquared = oneMinusT * oneMinusT;
            double tSquared = t * t;
            double twoOneMinusTt = 2d * oneMinusT * t;

            double x = oneMinusTSquared * startX +
                    twoOneMinusTt * controlX +
                    tSquared * end.x();
            double y = oneMinusTSquared * startY +
                    twoOneMinusTt * controlY +
                    tSquared * end.y();
            double z = oneMinusTSquared * startZ +
                    twoOneMinusTt * controlZ +
                    tSquared * end.z();

            level.sendParticles(
                    FrenziedFlame.FRENZY_PARTICLE_1,
                    x, y, z,
                    1,
                    0.0d, 0.0d, 0.0d,
                    0d
            );

            level.sendParticles(
                    FrenziedFlame.FRENZY_PARTICLE_2,
                    x, y, z,
                    3,
                    0.1d, 0.1d, 0.1d,
                    0d
            );
        }
    }

}
