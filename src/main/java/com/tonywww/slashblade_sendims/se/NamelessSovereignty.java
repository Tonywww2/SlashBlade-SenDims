package com.tonywww.slashblade_sendims.se;

import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/**
 * 王威 (Sovereignty) / 王威·极
 * <p>
 * 王者夺回的第一道权柄。斩击命中的敌人会被记入「王之名录」（同一目标不重复计入）；
 * 每 5 tick 结算一次：获得等同于名录长度的充能，随后清空名录。
 * 充能到达阈值时清空充能并恢复一定的最大 AP。
 * <ul>
 *     <li>王威：阈值 50，恢复 15% 最大 AP</li>
 *     <li>王威·极（由「无冠」进化）：阈值 40，恢复 20% 最大 AP</li>
 * </ul>
 */
public class NamelessSovereignty extends SpecialEffect {

    public NamelessSovereignty() {
        super(50);
    }

    /**
     * 斩击命中时把目标登记进「王之名录」。
     */
    public static void onHit(ServerPlayer player, LivingEntity target) {
        CompoundTag data = player.getPersistentData();
        long[] targets = data.getLongArray(NamelessSeries.SOVEREIGNTY_TARGETS);
        data.putLongArray(NamelessSeries.SOVEREIGNTY_TARGETS,
                NamelessSeries.appendUnique(targets, target.getUUID()));
    }

    /**
     * 每 5 tick 结算充能。由 {@code SEEventHandlers#onLivingTick} 调用。
     *
     * @param threshold 触发层数（王威 50 / 王威·极 40）
     * @param apRatio   触发时恢复的最大 AP 比例（0.15 / 0.20）
     */
    public static void onPlayerTick(ServerPlayer player, int threshold, double apRatio) {
        CompoundTag data = player.getPersistentData();

        int listed = data.getLongArray(NamelessSeries.SOVEREIGNTY_TARGETS).length / 2;
        int charge = data.getInt(NamelessSeries.SOVEREIGNTY_CHARGE);

        if (listed > 0) {
            charge += listed;
            data.remove(NamelessSeries.SOVEREIGNTY_TARGETS);
        }

        boolean triggered = false;
        if (charge >= threshold) {
            charge = 0;
            triggered = restoreActionPoint(player, apRatio);
        }

        data.putInt(NamelessSeries.SOVEREIGNTY_CHARGE, charge);

        if (triggered && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1.0d, player.getZ(),
                    20, 0.5d, 0.6d, 0.5d, 0.08d);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
    }

    /**
     * 恢复最大 AP 的一定比例。
     *
     * @return 是否成功恢复
     */
    private static boolean restoreActionPoint(ServerPlayer player, double ratio) {
        ItemStack soul = UmapyoiAPI.getUmaSoul(player);
        if (soul == null || soul.isEmpty()) {
            return false;
        }

        int maxAp = UmaSoulUtils.getMaxActionPoint(soul);
        int gain = (int) Math.round(maxAp * ratio);
        if (gain <= 0) {
            return false;
        }

        UmaSoulUtils.addActionPoint(soul, gain);
        return true;
    }
}
