package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.se.FrenziedFlame;
import com.tonywww.slashblade_sendims.utils.SlashBladeUtil;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class UnendurableFrenzy {

    public static double RANGE = 12.0d;

    public static void doUnendurableFrenzy(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel serverLevel)) return;
        if (!attacker.isAlive()) return;

        ItemStack bladeStack = attacker.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;

        ISlashBladeState state = SlashBladeUtil.getState(bladeStack);
        int expLevel = attacker instanceof Player player ? player.experienceLevel : 30;

        boolean withArcane = SpecialEffect.isEffective(SBSDSpecialEffects.ARCANE_A.get(), expLevel) &&
                state.hasSpecialEffect(SBSDSpecialEffects.ARCANE_A.getId());
        boolean withThreeFingers = SpecialEffect.isEffective(SBSDSpecialEffects.THREE_FINGERS.get(), expLevel) &&
                state.hasSpecialEffect(SBSDSpecialEffects.THREE_FINGERS.getId());

        int baseMadnessToTarget = FrenziedFlame.getFinalMadness(attacker, expLevel, withArcane, withThreeFingers);

        Vec3 eyePos = attacker.getEyePosition();
        Vec3 startPos = attacker.position().add(0, attacker.getBbHeight() + 0.2f, 0);
        Vec3 lookVec = attacker.getLookAngle();

        // Head Particles
        spawnHeadParticles(serverLevel, startPos);

        // Self Madness
        FrenziedFlame.addMadness(attacker, attacker, (int) Math.max(4, attacker.getMaxHealth() * 0.04f));

        // Get entities
        AABB searchBox = attacker.getBoundingBox().expandTowards(lookVec.scale(RANGE)).inflate(3.0d);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != attacker && !e.isSpectator() && e.isPickable());

        for (LivingEntity target : targets) {
            Vec3 toTarget = target.getEyePosition().subtract(eyePos).normalize();
            if (lookVec.dot(toTarget) > 0.70d) { // 约45度夹角
                if (serverLevel.random.nextFloat() < 0.75f) {
                    AttackManager.doMeleeAttack(attacker, target, true, false, 0.3f);
                    FrenziedFlame.addMadness(target, attacker, (int) (baseMadnessToTarget * 0.6f));

                    Vec3 endPos = target.position().add(0, target.getBbHeight(), 0);

                    FrenziedBurst.drawFrenziedParticleCurve(serverLevel, startPos, endPos, 32);
                    if (serverLevel.random.nextBoolean())FrenziedBurst.drawFrenziedParticleCurve(serverLevel, startPos, endPos, 16);
                }
            }
        }
    }

    private static void spawnHeadParticles(ServerLevel level, Vec3 headPos) {
        level.sendParticles(
                FrenziedFlame.FRENZY_PARTICLE_1,
                headPos.x, headPos.y, headPos.z,
                5,
                0.2d, 0.2d, 0.2d,
                0.02d
        );

        level.sendParticles(
                FrenziedFlame.FRENZY_PARTICLE_2,
                headPos.x, headPos.y, headPos.z,
                10,
                0.25d, 0.25d, 0.25d,
                0.05d
        );
    }
}