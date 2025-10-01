package com.tonywww.slashblade_sendims.utils;

import com.google.common.collect.Lists;
import mods.flammpfeil.slashblade.entity.IShootable;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.minecraftforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MobTargetSelector extends TargetSelector {

    public static class MobTargetingConditions extends TargetingConditions {
        public MobTargetingConditions(boolean pIsCombat) {
            super(pIsCombat);
        }

        @Override
        public boolean test(@Nullable LivingEntity attacker, LivingEntity target) {
            boolean isAttackable = false;
            LivingEntity targetLastAttacked = target.getLastHurtByMob();
            if (targetLastAttacked != null &&
                    attacker != null &&
                    (targetLastAttacked == attacker || targetLastAttacked.isAlliedTo(attacker))) {
                isAttackable = true;
            }

            if (!isAttackable && target instanceof Mob mob) {
                LivingEntity targetOfTarget = mob.getTarget();
                if (targetOfTarget != null &&
                        attacker != null &&
                        (targetOfTarget == attacker || targetOfTarget.isAlliedTo(attacker))) {
                    isAttackable = true;
                }
            }

            if (isAttackable) {
                target.addTag("RevengeAttacker");
            }

            return super.test(attacker, target);
        }
    }

    public static final TargetingConditions test = new MobTargetingConditions(true)
            .selector(new MobAttackablePredicate());
    public static final TargetingConditions areaAttack = new MobTargetingConditions(true)
            .range(12.0)
            .ignoreInvisibilityTesting()
            .selector(new MobAttackablePredicate());

    public static class MobAttackablePredicate implements Predicate<LivingEntity> {
        public MobAttackablePredicate() {
        }

        public boolean test(LivingEntity livingentity) {
            if (livingentity instanceof ArmorStand armorStand) {
                return armorStand.isMarker();
            } else if (livingentity.hasPassenger((entity) -> entity.isAlliedTo(livingentity))) {
                return false;
            } else if (livingentity.isCurrentlyGlowing()) {
                return true;
            } else if (livingentity.getTags().contains("RevengeAttacker")) {
                livingentity.removeTag("RevengeAttacker");
                return true;
            } else if (livingentity.getTeam() != null) {
                return true;
            } else {
                return true;
            }
        }
    }

    public static TargetingConditions getAreaAttackPredicate(double reach) {
        return MobTargetSelector.areaAttack.range(reach);
    }

    public static List<Entity> getTargettableEntitiesWithinAABB(Level world, LivingEntity attacker) {
        return MobTargetSelector.getTargettableEntitiesWithinAABB(world, attacker, getResolvedAxisAligned(attacker.getBoundingBox(), attacker.getLookAngle(), getResolvedReach(attacker)));
    }

    public static List<Entity> getTargettableEntitiesWithinAABB(Level world, LivingEntity attacker, AABB aabb) {
//        double reach = getResolvedReach(attacker);
        double reach = 8.0f;
        return MobTargetSelector.getTargettableEntitiesWithinAABB(world, attacker, aabb, reach);
    }

    public static <E extends Entity & IShootable> List<Entity> getTargettableEntitiesWithinAABB(Level world, double reach, E owner) {
        AABB aabb = owner.getBoundingBox().inflate(reach);
        List<Entity> list1 = Lists.newArrayList();
        list1.addAll(world.getEntitiesOfClass(EnderDragon.class, aabb.inflate(5.0)).stream()
                .flatMap((d) -> Arrays.stream(d.getSubEntities()))
                .filter((e) -> e.distanceToSqr(owner) < reach * reach).toList());
        LivingEntity user;
        if (owner.getShooter() instanceof LivingEntity) {
            user = (LivingEntity) owner.getShooter();
        } else {
            user = null;
        }

        list1.addAll(MobTargetSelector.getReflectableEntitiesWithinAABB(world, reach, owner));
        TargetingConditions predicate = MobTargetSelector.getAreaAttackPredicate(0.0);
        list1.addAll(world.getEntitiesOfClass(LivingEntity.class, aabb, (e) -> true).stream()
                .filter((t) -> predicate.test(user, t)).toList());
        return list1;
    }

    public static List<Entity> getTargettableEntitiesWithinAABB(Level world, LivingEntity attacker, AABB aabb, double reach) {
        List<Entity> out = Lists.newArrayList();
        out.addAll(getReflectableEntitiesWithinAABB(attacker));
        out.addAll(getExtinguishableEntitiesWithinAABB(attacker));
        List<Entity> mutipartsTargets = world.getEntitiesOfClass(LivingEntity.class, aabb.inflate(8.0), IForgeEntity::isMultipartEntity).stream()
                .flatMap((e) -> e.isMultipartEntity() ?
                        Stream.of(e.getParts()) :
                        Stream.of(e))
                .filter((t) -> {
                    boolean result = false;
                    MobAttackablePredicate check = new MobAttackablePredicate();
                    if (t instanceof LivingEntity living) {
                        result = check.test(living);
                    } else if (t instanceof PartEntity<?> part) {
                        Entity temp = part.getParent();
                        if (temp instanceof LivingEntity living) {
                            result = check.test(living) && part.distanceToSqr(attacker) < reach * reach;
                        }
                    }

                    return result;
                }).toList();
        out.addAll(mutipartsTargets);
        TargetingConditions predicate = MobTargetSelector.getAreaAttackPredicate(reach);
        List<LivingEntity> normalTargets = world.getEntitiesOfClass(LivingEntity.class, aabb);
        List<Entity> finalNormalTargets = normalTargets.stream()
                .flatMap((e) -> e.isMultipartEntity() ?
                        Stream.of(e.getParts()) :
                        Stream.of(e)).filter((t) -> {
                    boolean result = false;
                    if (t instanceof LivingEntity living) {
                        result = predicate.test(attacker, living);
                    } else if (t instanceof PartEntity<?> part) {
                        Entity temp = part.getParent();
                        if (temp instanceof LivingEntity living) {
                            result = predicate.test(attacker, living) && part.distanceToSqr(attacker) < reach * reach;
                        }
                    }

                    return result;
                }).toList();
        out.addAll(finalNormalTargets);
        return out;
    }

}
