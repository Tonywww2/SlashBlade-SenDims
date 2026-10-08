package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.leader.LeaderStateStorage;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 王权压制 (Royal Suppression)
 * <p>
 * 王者夺回的第三道权柄。对精英与 Boss 的斩击命中会额外造成
 * 相当于原伤害 10% 的真实伤害（无视护甲、附魔、药水与抗性）。
 */
public class NamelessSuppression extends SpecialEffect {

    public NamelessSuppression() {
        super(75);
    }

    /**
     * 斩击命中造成伤害时结算。由 {@code SEEventHandlers#onLivingHurt} 调用。
     *
     * @param originalDamage 本次斩击的原始伤害
     */
    public static void onLivingHurt(ServerPlayer player, LivingEntity target, float originalDamage) {
        if (target == player || !target.isAlive()) {
            return;
        }

        // 「精英怪与 Boss」：本包以 Leader 体系（Apotheosis 首领 / 精英）为判定
        if (!LeaderStateStorage.isLeader(target)) {
            return;
        }

        NamelessSeries.dealTrueDamage(player, target,
                originalDamage * NamelessSeries.SUPPRESSION_TRUE_DAMAGE_RATIO);
    }
}
