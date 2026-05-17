package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.utils.SlashBladeUtil;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntityJudgementCut;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class Mahakala {

    public static double RADIUS = 12d;
    public static double HEIGHT = 4d;
    public static double FORCE_SCALE = 1.5d;

    public static class MahakalaData {
        public Vec3 center;
        public Set<LivingEntity> targets;

        public MahakalaData(Vec3 center, Set<LivingEntity> targets) {
            this.center = center;
            this.targets = targets;
        }
    }

    private static final Map<UUID, MahakalaData> DATA_MAP = new HashMap<>();

    public static final DustColorTransitionOptions BLACK_SMOKE = new DustColorTransitionOptions(
            new Vector3f(0.1f, 0.1f, 0.1f),
            new Vector3f(0.0f, 0.0f, 0.0f),
            1.5f
    );
    public static final DustColorTransitionOptions WHITE_SMOKE = new DustColorTransitionOptions(
            new Vector3f(1.0f, 1.0f, 1.0f),
            new Vector3f(0.8f, 0.8f, 0.8f),
            1.0f
    );

    public static void start(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;
        Vec3 center;
        ISlashBladeState state = SlashBladeUtil.getState(attacker.getMainHandItem());
        Entity target = state != null ? state.getTargetEntity(serverLevel) : null;

        if (target != null) {
            double targetY = target.position().y;
            double selfY = attacker.position().y;

            if (targetY > selfY - HEIGHT && targetY < selfY + HEIGHT) {
                center = new Vec3(target.position().x, targetY + target.getBbHeight() + HEIGHT, target.position().z);
            } else {
                center = new Vec3(target.position().x, targetY + target.getBbHeight() / 2d, target.position().z);
            }
        } else {
            center = attacker.position()
                    .add(0, attacker.getBbHeight() + 2.5d, 0)
                    .add(attacker.getLookAngle().scale(6.0));
        }
        DATA_MAP.put(attacker.getUUID(), new MahakalaData(center, new HashSet<>()));
    }

    public static void pullAndAttack(LivingEntity attacker, int phase) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;
        MahakalaData data = DATA_MAP.get(attacker.getUUID());
        if (data == null) return;

        double radius = 3d + phase * 1d;
        int shellParticles = 80 + phase * 20;

        double phi = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < shellParticles; i++) {
            double y = 1.0 - (i / (double) (shellParticles - 1)) * 2.0;
            double r = Math.sqrt(1.0 - y * y);
            double theta = phi * i;

            double px = data.center.x + Math.cos(theta) * r * radius;
            double py = data.center.y + y * radius;
            double pz = data.center.z + Math.sin(theta) * r * radius;

            serverLevel.sendParticles(BLACK_SMOKE, px, py, pz, 1, 0, 0, 0, 0);
        }

        serverLevel.sendParticles(BLACK_SMOKE,
                data.center.x, data.center.y, data.center.z,
                (int) (20 + radius * 10),
                radius / 3.0, radius / 3.0, radius / 3.0,
                0.01);
        serverLevel.sendParticles(WHITE_SMOKE,
                data.center.x, data.center.y, data.center.z,
                (int) (5 + radius * 5),
                radius / 4.0, radius / 4.0, radius / 4.0,
                0.01);

        List<LivingEntity> newTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(data.center, data.center)
                .inflate(RADIUS), e -> e != attacker && e.isAlive());
        data.targets.addAll(newTargets);

        int targetCount = 0;
        for (LivingEntity e : data.targets) {
            if (e.isAlive()) targetCount++;
        }

        float multiplier = Math.max(7.0f, 12.0f - (targetCount - 1f) * 0.75f) / 5f;

        for (LivingEntity e : data.targets) {
            if (!e.isAlive()) continue;

            Vec3 ePos = e.position().add(0, e.getBbHeight() / 2, 0);
            Vec3 dirToCenter = data.center.subtract(ePos);
            double dist = dirToCenter.length();
            int points = (int) (dist * 4);
            if (points > 0) {
                Vec3 step = dirToCenter.scale(1.0 / points);
                for (int i = 0; i <= points; i++) {
                    Vec3 p = ePos.add(step.scale(i));
                    serverLevel.sendParticles(BLACK_SMOKE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }

            AttackManager.doMeleeAttack(attacker, e, true, false, multiplier);

            Vec3 dir = dirToCenter.normalize();
            e.setDeltaMovement(dir.scale(FORCE_SCALE));
            e.hurtMarked = true;
        }
    }

    public static void doTick4(LivingEntity attacker) {
        start(attacker);
        pullAndAttack(attacker, 1);
    }

    public static void doTick7(LivingEntity attacker) {
        pullAndAttack(attacker, 2);
    }

    public static void doTick10(LivingEntity attacker) {
        pullAndAttack(attacker, 3);
    }

    public static void doTick13(LivingEntity attacker) {
        pullAndAttack(attacker, 4);
    }

    public static void doTick16(LivingEntity attacker) {
        pullAndAttack(attacker, 5);
    }

    public static void doTickFinal(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;
        MahakalaData data = DATA_MAP.get(attacker.getUUID());
        if (data == null) return;

        EntityJudgementCut slash = new EntityJudgementCut(SlashBlade.RegistryEvents.JudgementCut, serverLevel);
        slash.setPos(data.center.x, data.center.y, data.center.z);
        slash.setDamage(0.2f);
        slash.setColor(0x050505);
        slash.setOwner(attacker);
        slash.setYRot(attacker.getYRot());
        slash.setXRot(attacker.getXRot());
        serverLevel.addFreshEntity(slash);

        serverLevel.sendParticles(BLACK_SMOKE, data.center.x, data.center.y, data.center.z, 100, 2.0, 2.0, 2.0, 0.1);

        DATA_MAP.remove(attacker.getUUID());
    }
}
