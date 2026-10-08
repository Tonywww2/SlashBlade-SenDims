package com.tonywww.slashblade_sendims.se;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * 褪名 (Faded Name)
 * <p>
 * 无名系列最初的代价：王者舍弃了自己的名字，刀也因此无法被完整地持守。
 * 若 5 秒内未斩击命中任何敌人，下一次命中会强行修正刀的耐久——
 * 高于 70% 则降至 70%，低于 30% 则抬升至 30%。
 */
public class NamelessFaded extends SpecialEffect {

    public NamelessFaded() {
        super(35);
    }

    /**
     * 斩击命中时结算。由 {@code SEEventHandlers#onHit} 调用。
     */
    public static void onHit(ServerPlayer player, ISlashBladeState state) {
        if (!NamelessSeries.markFadedHit(player)) {
            return;
        }

        int max = state.getMaxDamage();
        if (max <= 0) {
            return;
        }

        float current = 1.0f - (float) state.getDamage() / (float) max;
        float target;
        if (current > NamelessSeries.FADED_HIGH_RATIO) {
            target = NamelessSeries.FADED_HIGH_RATIO;
        } else if (current < NamelessSeries.FADED_LOW_RATIO) {
            target = NamelessSeries.FADED_LOW_RATIO;
        } else {
            return;
        }

        if (!NamelessSeries.clampDurability(player, state, target)) {
            return;
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 1.0d, player.getZ(),
                    10, 0.35d, 0.45d, 0.35d, 0.02d);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.45f, 0.65f);
        }
    }
}
