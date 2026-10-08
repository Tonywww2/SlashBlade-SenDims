package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.registeries.SBSDMobEffects;
import com.tonywww.slashblade_sendims.sa.NamelessThreefold;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 风暴 (Storm)
 * <p>
 * 王者夺回的第二道权柄。释放 SA 时：
 * <ul>
 *     <li>获得 2 秒「风暴穿透」（V 级，+25 魔法穿透）</li>
 *     <li>对锁定目标（无锁定则取最近敌人）落下一道雷击</li>
 * </ul>
 */
public class NamelessStorm extends SpecialEffect {

    public NamelessStorm() {
        super(65);
    }

    /**
     * SA 释放时结算。由 {@code SEEventHandlers} 监听 {@code SlashBladeEvent.ChargeActionEvent} 调用。
     */
    public static void onChargeAction(ServerPlayer player, ISlashBladeState state) {
        // 1) 风暴穿透
        player.addEffect(new MobEffectInstance(
                SBSDMobEffects.STORM_PENETRATION.get(),
                NamelessSeries.STORM_EFFECT_TICKS,
                NamelessSeries.STORM_EFFECT_AMPLIFIER,
                false, false, true
        ));

        // 2) 落雷
        LivingEntity target = resolveTarget(player, state);
        if (target == null) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 pos = target.position();

        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 0.6f, 1.45f);
        NamelessSeries.spawnLightningVisual(level, pos);

        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * NamelessSeries.STORM_LIGHTNING_DAMAGE_RATIO;
        if (damage <= 0f) {
            damage = 1.0f;
        }
        damage = NamelessThreefold.limitStormDamage(player, target, damage);

        // 以玩家为归属的闪电伤害
        DamageSource source = new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.LIGHTNING_BOLT),
                player
        );
        target.invulnerableTime = 0;
        target.hurt(source, damage);
    }

    /**
     * 优先取拔刀剑的锁定目标，否则取最近的敌人。
     */
    private static LivingEntity resolveTarget(ServerPlayer player, ISlashBladeState state) {
        Entity locked = state.getTargetEntity(player.level());
        if (locked instanceof LivingEntity living && living.isAlive() && living != player) {
            return living;
        }

        List<Entity> candidates = TargetSelector.getTargettableEntitiesWithinAABB(player.level(), player);
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Entity candidate : candidates) {
            if (!(candidate instanceof LivingEntity living) || !living.isAlive() || living == player) {
                continue;
            }
            double distance = living.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = living;
            }
        }
        return nearest;
    }
}
