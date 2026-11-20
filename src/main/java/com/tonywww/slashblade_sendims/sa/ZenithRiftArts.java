package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.events.SuperSlashArtsReleaseEvent;
import com.tonywww.slashblade_sendims.network.ZenithRiftPacket;
import com.tonywww.slashblade_sendims.registeries.SBSDMobEffects;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackHelper;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 天顶·裂空的 SA「真·次元斩」（设计见整合包 docs/development/最终天顶剑设计-2026-11-14.md）。
 * <p>
 * 时间线（tick）：0 起势，8–40 乱斩（每 2 tick 一次，共 16 次），40 定格，56 收刀，60 碎裂并结算终结伤害，80 解除定身与凝滞。
 * 画面全在客户端按时间表播放（{@link ZenithRiftPacket}），服务端只负责判定、伤害和音效。
 * 施放期间施放者无敌、定身（空中释放时悬停）；目标进入「次元凝滞」：首领和玩家减速 80%，其他目标完全定住。
 * 一旦放出就会放完：打开背包等界面、切换手持物品都不会打断，只有施放者死亡、离开维度或下线时提前结束。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class ZenithRiftArts {
    public static final ResourceLocation ID = SenDims.prefix("true_judgement_cut");

    public static final int SLASH_START = 8;
    public static final int SLASH_END = 40;
    public static final int SLASH_INTERVAL = 2;
    public static final int FREEZE = 40;
    public static final int SHEATH = 56;
    public static final int SHATTER = 60;
    public static final int RELEASE = 80;
    /** 客户端画面的总长度（服务端在 RELEASE 结束）。 */
    public static final int DURATION = 90;

    public static final float SLASH_RATIO = 0.5F;
    public static final float FINISH_PHYSICAL = 7.0F;
    public static final float FINISH_MAGIC = 7.0F;
    public static final double RADIUS = 24.0D;
    public static final double HALF_HEIGHT = 12.0D;
    public static final double VIEW_DISTANCE = 64.0D;

    private static final UUID HOLD_SPEED = UUID.fromString("6f1d2c3a-8b4e-4f5a-9c7d-1e2f3a4b5c61");
    private static final UUID HOLD_GRAVITY = UUID.fromString("6f1d2c3a-8b4e-4f5a-9c7d-1e2f3a4b5c62");

    private static final Map<UUID, Cast> CASTS = new HashMap<>();
    private static long nextId;

    private static final class Cast {
        final LivingEntity user;
        final ServerLevel level;
        final ItemStack blade;
        final long started;
        final long id;
        final long seed;
        final Vec3 origin;
        final Map<UUID, Double> baseDamage = new HashMap<>();

        Cast(LivingEntity user, ServerLevel level) {
            this.user = user;
            this.level = level;
            this.blade = user.getMainHandItem();
            this.started = level.getGameTime();
            this.id = ++nextId;
            this.seed = level.getRandom().nextLong();
            this.origin = user.position();
        }

        boolean valid() {
            return user.isAlive() && !user.isRemoved() && user.level() == level;
        }

        /** 优先用手上的刀；施放中换了手持物品时用施放时的刀。 */
        ISlashBladeState state() {
            ISlashBladeState state = NamelessSeries.getBladeState(user);
            return state != null ? state : blade.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
        }

        double base(LivingEntity target) {
            return baseDamage.computeIfAbsent(target.getUUID(), ignored -> SukunaCombat.base(user, target));
        }

        void visual(ZenithRiftPacket.Stage stage) {
            ZenithRiftPacket.send(level, new ZenithRiftPacket(level.dimension().location(), id, stage, user.getId(),
                    origin.x, origin.y, origin.z, (float) RADIUS, seed));
        }
    }

    private ZenithRiftArts() {
    }

    public static boolean casting(LivingEntity entity) {
        return CASTS.containsKey(entity.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void release(SlashBladeEvent.PerformSlashArtEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (!ID.equals(event.getComboState()) || user.level().isClientSide()
                || event.getType() == SlashArts.ArtsType.Fail) return;
        ISlashBladeState state = event.getSlashBladeState();
        var current = state == null ? null : ComboStateRegistry.REGISTRY.get().getValue(
                state.resolvCurrentComboStateTicks(user).getValue());
        if (state == null || state.isBroken() || state.isSealed() || current == null || current.getPriority() <= 50
                || casting(user)) {
            event.setCanceled(true);
        }
    }

    /** 不做超级 SA：真·次元斩只有一个版本。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void superRelease(SuperSlashArtsReleaseEvent event) {
        ISlashBladeState state = NamelessSeries.getBladeState(event.getPlayer());
        if (state != null && ID.equals(state.getSlashArtsKey())) event.setCanceled(true);
    }

    /** 原生连招在 SA 成功释放、AP 扣除之后调用。 */
    public static void begin(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel level) || casting(user)) return;
        ISlashBladeState state = NamelessSeries.getBladeState(user);
        if (state == null || !ID.equals(state.getComboSeq())) return;
        Cast cast = new Cast(user, level);
        CASTS.put(user.getUUID(), cast);
        hold(user, true);
        cast.visual(ZenithRiftPacket.Stage.START);
        sound(cast, SoundEvents.BEACON_ACTIVATE, 1.0F, 0.55F);
        sound(cast, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.6F);
        tick(cast, 0);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CASTS.isEmpty()) return;
        Iterator<Cast> it = CASTS.values().iterator();
        while (it.hasNext()) {
            Cast cast = it.next();
            long elapsed = cast.level.getGameTime() - cast.started;
            if (!cast.valid()) {
                cast.visual(ZenithRiftPacket.Stage.END);
                hold(cast.user, false);
                it.remove();
            } else if (elapsed >= RELEASE) {
                hold(cast.user, false);
                it.remove();
            } else if (elapsed > 0) {
                tick(cast, (int) elapsed);
            }
        }
    }

    private static void tick(Cast cast, int elapsed) {
        LivingEntity user = cast.user;
        user.setDeltaMovement(Vec3.ZERO);
        user.fallDistance = 0.0F;
        if (user instanceof ServerPlayer) user.hurtMarked = true;

        List<LivingEntity> targets = targets(cast);
        int stasis = RELEASE - elapsed + 2;
        for (LivingEntity target : targets) {
            boolean partial = target instanceof Player || target.getType().is(Tags.EntityTypes.BOSSES);
            target.addEffect(new MobEffectInstance(SBSDMobEffects.DIMENSION_STASIS.get(), stasis, partial ? 0 : 1,
                    false, false, true), user);
            if (!partial) target.setDeltaMovement(Vec3.ZERO);
        }

        if (elapsed >= SLASH_START && elapsed < SLASH_END && (elapsed - SLASH_START) % SLASH_INTERVAL == 0) {
            int hit = (elapsed - SLASH_START) / SLASH_INTERVAL;
            for (LivingEntity target : targets) damage(cast, target, SLASH_RATIO, 0.0F);
            sound(cast, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.1F + hit * 0.04F);
            if (hit % 4 == 0) sound(cast, SoundEvents.GLASS_BREAK, 0.45F, 1.6F + hit * 0.02F);
        }
        if (elapsed == FREEZE) {
            sound(cast, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.5F);
            sound(cast, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.6F);
        }
        if (elapsed == SHEATH) sound(cast, SoundEvents.CHAIN_PLACE, 1.2F, 1.7F);
        if (elapsed == SHATTER) {
            for (LivingEntity target : targets) damage(cast, target, FINISH_PHYSICAL, FINISH_MAGIC);
            sound(cast, SoundEvents.GLASS_BREAK, 1.5F, 0.6F);
            sound(cast, SoundEvents.AMETHYST_BLOCK_BREAK, 1.5F, 0.5F);
            sound(cast, SoundEvents.WARDEN_SONIC_BOOM, 0.7F, 1.4F);
        }
    }

    /** 拔刀剑的目标筛选（友伤与队伍规则照常），再限制在以起点为中心的圆柱内。 */
    private static List<LivingEntity> targets(Cast cast) {
        AABB box = new AABB(cast.origin, cast.origin).inflate(RADIUS, HALF_HEIGHT, RADIUS);
        List<Entity> found = TargetSelector.getTargettableEntitiesWithinAABB(cast.level, cast.user, box,
                RADIUS + HALF_HEIGHT);
        List<LivingEntity> targets = new ArrayList<>(found.size());
        for (Entity entity : found) {
            if (!(entity instanceof LivingEntity living) || living == cast.user || !living.isAlive()) continue;
            AABB bounds = living.getBoundingBox();
            double dx = Math.max(0.0D, Math.max(bounds.minX - cast.origin.x, cast.origin.x - bounds.maxX));
            double dz = Math.max(0.0D, Math.max(bounds.minZ - cast.origin.z, cast.origin.z - bounds.maxZ));
            if (dx * dx + dz * dz > RADIUS * RADIUS) continue;
            if (bounds.maxY < cast.origin.y - HALF_HEIGHT || bounds.minY > cast.origin.y + HALF_HEIGHT) continue;
            targets.add(living);
        }
        return targets;
    }

    private static void damage(Cast cast, LivingEntity target, float physicalRatio, float magicRatio) {
        ISlashBladeState state = cast.state();
        if (state == null) return;
        boolean wasClicked = state.onClick();
        state.setOnClick(true);
        try {
            if (cast.user instanceof Player player && !ForgeHooks.onPlayerAttackTarget(player, target)) return;
            double base = cast.base(target);
            if (!Double.isFinite(base) || base <= 0.0D) return;
            float before = target.getHealth();
            boolean hit = false;
            if (physicalRatio > 0.0F) {
                target.invulnerableTime = 0;
                DamageSource physical = cast.user instanceof Player player ? cast.user.damageSources().playerAttack(player)
                        : cast.user.damageSources().mobAttack(cast.user);
                hit = target.hurt(physical, (float) (base * physicalRatio));
            }
            if (magicRatio > 0.0F && target.isAlive()) {
                target.invulnerableTime = 0;
                hit |= target.hurt(cast.user.damageSources().indirectMagic(cast.user, cast.user),
                        (float) (base * magicRatio));
            }
            if (hit) {
                AttackHelper.handleEnchantmentsAndDurability(cast.user, target);
                if (cast.user instanceof Player player) {
                    player.awardStat(Stats.DAMAGE_DEALT, Math.round(Math.max(0.0F, before - target.getHealth()) * 10));
                    player.causeFoodExhaustion(0.1F);
                }
            }
        } finally {
            state.setOnClick(wasClicked);
        }
    }

    /** 定身与悬停：临时属性修饰，不写进存档。 */
    private static void hold(LivingEntity user, boolean on) {
        AttributeInstance speed = user.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance gravity = user.getAttribute(ForgeMod.ENTITY_GRAVITY.get());
        if (speed != null) {
            speed.removeModifier(HOLD_SPEED);
            if (on) speed.addTransientModifier(new AttributeModifier(HOLD_SPEED, "sendims true judgement cut hold",
                    -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        if (gravity != null) {
            gravity.removeModifier(HOLD_GRAVITY);
            if (on) gravity.addTransientModifier(new AttributeModifier(HOLD_GRAVITY, "sendims true judgement cut hover",
                    -1.0D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void sound(Cast cast, SoundEvent sound, float volume, float pitch) {
        cast.level.playSound(null, cast.origin.x, cast.origin.y, cast.origin.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** 施放者无敌；完全凝滞的目标不能造成伤害。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (casting(event.getEntity()) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        } else if (frozen(source.getEntity()) || frozen(source.getDirectEntity())) {
            event.setCanceled(true);
        }
    }

    private static boolean frozen(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        MobEffectInstance effect = living.getEffect(SBSDMobEffects.DIMENSION_STASIS.get());
        return effect != null && effect.getAmplifier() >= 1;
    }

    @SubscribeEvent
    public static void knockBack(LivingKnockBackEvent event) {
        if (frozen(event.getEntity()) || casting(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        Cast cast = CASTS.remove(event.getEntity().getUUID());
        if (cast != null) {
            cast.visual(ZenithRiftPacket.Stage.END);
            hold(cast.user, false);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        CASTS.clear();
    }
}
