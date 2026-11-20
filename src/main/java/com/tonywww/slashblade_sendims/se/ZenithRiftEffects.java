package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.ZenithRiftPacket;
import com.tonywww.slashblade_sendims.registeries.SBSDMobEffects;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.sa.SukunaCombat;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.util.AttackHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 天顶·裂空的两个 SE（设计见整合包 docs/development/最终天顶剑设计-2026-11-14.md 第 6 节）。
 * <ul>
 *   <li>「天顶」：斩击时获得增幅 II（5 秒，持续斩击时续上）。手持这把刀时造成的伤害提高
 *       0.45 ×（1 − 0.975^(x + 1)），x 为刀上所有附魔的总等级：x = 0 时约 1.1%，锋利 10 加耐久 10 时约 18.6%，上限 45%。</li>
 *   <li>「裂空」：斩击命中时，在命中的目标处释放一次空间斩；有锁定目标时改为锁定目标处。
 *       空间斩对中心 3 格内的敌人各造成一次 0.5 倍的物理伤害，画面沿用真·次元斩的世界斩痕。
 *       同一 tick 只触发一次（1 tick 冷却），空间斩本身的伤害不会再触发。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class ZenithRiftEffects {
    public static final int AMPLIFY_TICKS = 100;
    /** 增幅 II。 */
    public static final int AMPLIFY_AMPLIFIER = 1;
    public static final double APEX_CAP = 0.45D;
    public static final double APEX_BASE = 0.975D;
    public static final float RIFT_RATIO = 0.5F;
    public static final double RIFT_RADIUS = 3.0D;
    public static final double LOCK_RANGE = 48.0D;

    private static final Map<UUID, Long> LAST_RIFT = new HashMap<>();
    private static final ThreadLocal<Boolean> RIFTING = ThreadLocal.withInitial(() -> false);
    private static long sequence;

    private ZenithRiftEffects() {
    }

    /** 「天顶」的增伤比例。 */
    public static double apexBonus(int enchantmentLevels) {
        return APEX_CAP * (1.0D - Math.pow(APEX_BASE, Math.max(0, enchantmentLevels) + 1));
    }

    private static boolean active(ServerPlayer player, ISlashBladeState state, RegistryObject<SpecialEffect> effect) {
        return state != null && !state.isBroken() && !state.isSealed()
                && SEEventHandlers.isSEActive(state, player.experienceLevel, effect);
    }

    // ---------------------------------------------------------------- 天顶

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void slash(SlashBladeEvent.DoSlashEvent event) {
        if (!(event.getUser() instanceof ServerPlayer player)
                || !active(player, event.getSlashBladeState(), SBSDSpecialEffects.ZENITH_APEX)) return;
        MobEffectInstance current = player.getEffect(SBSDMobEffects.AMPLIFY.get());
        if (current != null && (current.getAmplifier() > AMPLIFY_AMPLIFIER
                || current.getAmplifier() == AMPLIFY_AMPLIFIER && current.getDuration() > AMPLIFY_TICKS - 10)) return;
        player.addEffect(new MobEffectInstance(SBSDMobEffects.AMPLIFY.get(), AMPLIFY_TICKS, AMPLIFY_AMPLIFIER));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void damage(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!active(player, NamelessSeries.getBladeState(player), SBSDSpecialEffects.ZENITH_APEX)) return;
        int levels = 0;
        for (int level : EnchantmentHelper.getEnchantments(player.getMainHandItem()).values()) levels += level;
        event.setAmount((float) (event.getAmount() * (1.0D + apexBonus(levels))));
    }

    // ---------------------------------------------------------------- 裂空

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void hit(SlashBladeEvent.HitEvent event) {
        if (RIFTING.get() || !(event.getUser() instanceof ServerPlayer player)) return;
        ISlashBladeState state = event.getSlashBladeState();
        LivingEntity target = event.getTarget();
        if (target == null || !active(player, state, SBSDSpecialEffects.SKY_RIFT)) return;
        long now = player.level().getGameTime();
        Long last = LAST_RIFT.put(player.getUUID(), now);
        if (last != null && last == now) return;
        LivingEntity center = target;
        Entity locked = state.getTargetEntity(player.level());
        if (locked instanceof LivingEntity living && living != player && living.isAlive()
                && player.distanceToSqr(living) <= LOCK_RANGE * LOCK_RANGE) {
            center = living;
        }
        release(player, center);
    }

    private static void release(ServerPlayer player, LivingEntity center) {
        ServerLevel level = player.serverLevel();
        Vec3 origin = SukunaCombat.center(center);
        RIFTING.set(true);
        try {
            AABB box = new AABB(origin, origin).inflate(RIFT_RADIUS);
            for (Entity entity : TargetSelector.getTargettableEntitiesWithinAABB(level, player, box, RIFT_RADIUS * 2.0D)) {
                if (entity instanceof LivingEntity victim && victim != player && victim.isAlive()
                        && victim.getBoundingBox().distanceToSqr(origin) <= RIFT_RADIUS * RIFT_RADIUS) {
                    physical(player, victim, SukunaCombat.base(player, victim) * RIFT_RATIO);
                }
            }
        } finally {
            RIFTING.set(false);
        }
        ZenithRiftPacket.send(level, new ZenithRiftPacket(level.dimension().location(), ++sequence,
                ZenithRiftPacket.Stage.CUT, player.getId(), origin.x, origin.y, origin.z, (float) RIFT_RADIUS,
                level.getRandom().nextLong()));
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.6F, 1.5F + level.getRandom().nextFloat() * 0.2F);
    }

    private static void physical(ServerPlayer player, LivingEntity target, double amount) {
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        if (state == null || !Double.isFinite(amount) || amount <= 0.0D) return;
        boolean clicked = state.onClick();
        // 原生拔刀剑会把左键攻击转进连招，先标记为已点击。
        state.setOnClick(true);
        try {
            if (!ForgeHooks.onPlayerAttackTarget(player, target)) return;
            float before = target.getHealth();
            target.invulnerableTime = 0;
            if (target.hurt(player.damageSources().playerAttack(player), (float) amount)) {
                AttackHelper.handleEnchantmentsAndDurability(player, target);
                player.awardStat(Stats.DAMAGE_DEALT, Math.round(Math.max(0.0F, before - target.getHealth()) * 10));
            }
        } finally {
            state.setOnClick(clicked);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_RIFT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        LAST_RIFT.clear();
    }
}
