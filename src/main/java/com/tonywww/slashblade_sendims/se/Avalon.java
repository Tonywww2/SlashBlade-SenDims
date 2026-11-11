package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDMobEffects;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import mods.flammpfeil.slashblade.ability.Untouchable;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * 阿瓦隆（誓约胜利之剑）：蓄力到第 15 tick 时获得 20 tick 无敌，冷却 120 tick。
 * 无敌直接调用拔刀剑闪避使用的 {@link Untouchable#setUntouchable}，同时挂一个同时长的药水效果用于显示。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class Avalon {
    public static final int TRIGGER_CHARGE_TICKS = 15;
    public static final int INVULNERABLE_TICKS = 20;
    public static final int COOLDOWN_TICKS = 120;
    /** 玩家 PersistentData：下一次可触发的游戏时间 */
    public static final String NEXT_READY = "sbsd.se.avalon.next_ready";

    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.86F, 0.45F), 1.0F);
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.45F, 0.65F, 1.0F), 0.9F);

    private Avalon() {}

    @SubscribeEvent
    public static void onCharge(SlashBladeEvent.ChargeActionEvent event) {
        if (!(event.getEntityLiving() instanceof ServerPlayer player)) return;
        if (event.getChargeTicks() != TRIGGER_CHARGE_TICKS) return;
        ISlashBladeState state = event.getSlashBladeState();
        if (state == null || state.isBroken() || state.isSealed()
                || !SEEventHandlers.isSEActive(state, player.experienceLevel, SBSDSpecialEffects.AVALON)) return;
        tryGrant(player);
    }

    /** 按冷却尝试触发，成功返回 true。 */
    public static boolean tryGrant(ServerPlayer player) {
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLong(NEXT_READY)) return false;
        player.getPersistentData().putLong(NEXT_READY, now + COOLDOWN_TICKS);
        // 先挂显示效果，Untouchable 会把当前效果集记为"闪避前已有"，不会在受击后被清掉。
        player.addEffect(new MobEffectInstance(SBSDMobEffects.AVALON.get(), INVULNERABLE_TICKS, 0, false, false, true));
        Untouchable.setUntouchable(player, INVULNERABLE_TICKS);
        var level = player.serverLevel();
        level.sendParticles(GOLD, player.getX(), player.getY() + 1.0D, player.getZ(), 26, 0.5D, 0.8D, 0.5D, 0.02D);
        level.sendParticles(BLUE, player.getX(), player.getY() + 1.0D, player.getZ(), 12, 0.4D, 0.7D, 0.4D, 0.01D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.6F);
        return true;
    }
}
