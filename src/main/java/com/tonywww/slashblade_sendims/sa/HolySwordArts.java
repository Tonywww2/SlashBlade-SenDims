package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.HolySwordVisualPacket.Stage;
import com.tonywww.slashblade_sendims.events.SuperSlashArtsReleaseEvent;
import com.tonywww.slashblade_sendims.se.HolySwordRules;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Caliburn: 3+10 physical. Excalibur: (2+2)+(2+2)+(3+3), physical+magic. */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class HolySwordArts {
    public static final ResourceLocation CALIBURN = SenDims.prefix("caliburn");
    public static final ResourceLocation CALIBURN_FINISH = SenDims.prefix("caliburn_finish");
    public static final ResourceLocation EXCALIBUR = SenDims.prefix("excalibur");
    public static final int CALIBURN_CHARGE_TICKS = 9;
    public static final int EXCALIBUR_CHARGE_TICKS = 18;
    private static final Map<UUID, Cast> CASTS = new HashMap<>();

    private HolySwordArts() {}

    private static final class Cast {
        final LivingEntity user;
        final ServerLevel level;
        final ItemStack blade;
        final ResourceLocation art;
        final long started;
        final Vec3 origin;
        final Vec3 direction;
        final Map<UUID, Double> baseDamage = new HashMap<>();
        int completed;
        boolean finished;

        Cast(LivingEntity user, ServerLevel level, ResourceLocation art) {
            this.user = user; this.level = level; this.art = art;
            blade = user.getMainHandItem(); started = level.getGameTime();
            origin = user.getEyePosition().add(0, -0.25D, 0);
            Vec3 look = user.getLookAngle();
            direction = EXCALIBUR.equals(art) ? look.normalize() : horizontal(look, user.getYRot());
        }

        double base(LivingEntity target) {
            return baseDamage.computeIfAbsent(target.getUUID(), id -> Math.max(0.0D,
                    AttackHelper.calculateTotalDamage(user, target, 1.0F, false)));
        }

        boolean valid() {
            ISlashBladeState state = NamelessSeries.getBladeState(user);
            return user.isAlive() && !user.isRemoved() && user.level() == level
                    && user.getMainHandItem() == blade && state != null && !state.isBroken() && !state.isSealed()
                    && (art.equals(state.getComboSeq())
                    || (CALIBURN.equals(art) && CALIBURN_FINISH.equals(state.getComboSeq())))
                    && level.getGameTime() - started <= 40;
        }
    }

    public static boolean isArt(ResourceLocation id) { return CALIBURN.equals(id) || EXCALIBUR.equals(id); }
    public static boolean isCombo(ResourceLocation id) { return isArt(id) || CALIBURN_FINISH.equals(id); }
    public static boolean isCasting(LivingEntity user) {
        Cast cast = CASTS.get(user.getUUID());
        return cast != null && cast.valid();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void checkRelease(SlashBladeEvent.PerformSlashArtEvent event) {
        if (event.getEntityLiving().level().isClientSide() || !isArt(event.getComboState())
                || event.getType() == SlashArts.ArtsType.Fail) return;
        ISlashBladeState state = event.getSlashBladeState();
        ComboState current = state == null ? null : ComboStateRegistry.REGISTRY.get()
                .getValue(state.resolvCurrentComboStateTicks(event.getEntityLiving()).getValue());
        // Native priority rules would reject this entry. Cancel BEFORE AP is spent.
        if (state == null || state.isBroken() || state.isSealed() || current == null
                || current.getPriority() <= 50 || isCasting(event.getEntityLiving())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void checkSuperRelease(SuperSlashArtsReleaseEvent event) {
        ServerPlayer player = event.getPlayer();
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        if (state == null || !isArt(state.getSlashArtsKey())) return;
        ComboState current = ComboStateRegistry.REGISTRY.get().getValue(state.resolvCurrentComboStateTicks(player).getValue());
        var types = SwordType.from(player.getMainHandItem());
        if (!player.onGround() || state.isBroken() || state.isSealed() || state.getDamage() > 0
                || !types.contains(SwordType.BEWITCHED) || !types.contains(SwordType.FIERCEREDGE)
                || current == null || current.getPriority() <= 50 || isCasting(player)) event.setCanceled(true);
    }

    /** The native state calls clickAction only after successful release and AP checks. */
    public static void begin(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel level)
                || !(user.getMainHandItem().getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = NamelessSeries.getBladeState(user);
        if (state == null || !isArt(state.getComboSeq())) return;
        // Release guards already rejected a live cast before native combo entry.
        // Replace a completed cast here: after entering the same combo its old
        // snapshot could otherwise look valid again until the next cleanup tick.
        Cast cast = new Cast(user, level, state.getComboSeq());
        CASTS.put(user.getUUID(), cast);
        if (EXCALIBUR.equals(cast.art)) {
            sound(cast, SoundEvents.BEACON_ACTIVATE, 1.2F, 1.4F);
            sound(cast, SoundEvents.TRIDENT_THUNDER, 0.7F, 1.3F);
        } else {
            HolySwordVisuals.phase(user, cast.started, false, Stage.OPEN, cast.origin,
                    cast.origin.add(cast.direction.scale(4)));
            sound(cast, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.65F);
        }
        tick(user);
    }

    public static void tick(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel)) return;
        Cast cast = CASTS.get(user.getUUID());
        if (cast == null || !cast.valid()) return;
        if (EXCALIBUR.equals(cast.art)) {
            long elapsed = ComboState.getElapsed(user);
            int[] times = {0, 4, 10};
            for (int phase = 0; phase < times.length; phase++) {
                if (elapsed >= times[phase] && (cast.completed & (1 << phase)) == 0) {
                    cast.completed |= 1 << phase;
                    beamPulse(cast, phase);
                }
            }
        } else if ((cast.completed & 1) == 0) {
            cast.completed |= 1;
            opening(cast);
        }
    }

    /** The second animation belongs to the same cast and does not consume AP/charge again. */
    public static void finishCaliburn(LivingEntity user) {
        Cast cast = CASTS.get(user.getUUID());
        if (cast == null || !cast.valid() || cast.finished) return;
        cast.finished = true;
        Vec3 end = clippedEnd(cast, 10.0D);
        for (LivingEntity target : cast.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(cast.origin, end).inflate(2.0D, 2.0D, 2.0D), other -> enemy(cast.user, other))) {
            if (insideBeam(cast, target, end, 2.0D) && visible(cast, target)) {
                damage(cast, target, HolySwordRules.CALIBURN_FINISH, 0.0F);
            }
        }
        HolySwordVisuals.phase(user, cast.started, false, Stage.FINISH, cast.origin, end);
        sound(cast, SoundEvents.PLAYER_ATTACK_STRONG, 1.3F, 0.65F);
        sound(cast, SoundEvents.TRIDENT_RIPTIDE_2, 0.9F, 1.1F);
    }

    private static void opening(Cast cast) {
        for (LivingEntity target : cast.level.getEntitiesOfClass(LivingEntity.class,
                cast.user.getBoundingBox().inflate(4.0D, 2.0D, 4.0D), other -> enemy(cast.user, other))) {
            Vec3 offset = center(target).subtract(cast.origin);
            if (offset.horizontalDistanceSqr() <= 16.0D && Math.abs(offset.y) <= 2.5D
                    && offset.dot(cast.direction) >= 0 && visible(cast, target)) {
                damage(cast, target, HolySwordRules.CALIBURN_OPENING, 0.0F);
            }
        }
    }

    private static void beamPulse(Cast cast, int phase) {
        Vec3 end = clippedEnd(cast, 36.0D);
        for (LivingEntity target : cast.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(cast.origin, end).inflate(2.0D), other -> enemy(cast.user, other))) {
            if (insideBeam(cast, target, end, 2.0D) && visible(cast, target)) {
                damage(cast, target, HolySwordRules.excaliburPhysical(phase), HolySwordRules.excaliburMagic(phase));
            }
        }
        HolySwordVisuals.phase(cast.user, cast.started, true,
                phase == 0 ? Stage.PULSE_0 : phase == 1 ? Stage.PULSE_1 : Stage.PULSE_2, cast.origin, end);
        if (phase == 2) sound(cast, SoundEvents.GENERIC_EXPLODE, 1.0F, 1.25F);
    }

    private static boolean enemy(LivingEntity user, LivingEntity target) {
        return target != user && target.isAlive() && !target.isSpectator() && !user.isAlliedTo(target)
                && target.isAttackable() && !target.skipAttackInteraction(user)
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }

    private static Vec3 horizontal(Vec3 look, float yaw) {
        Vec3 flat = look.multiply(1, 0, 1);
        return flat.lengthSqr() < 1.0E-6D ? Vec3.directionFromRotation(0, yaw) : flat.normalize();
    }
    private static Vec3 center(Entity entity) { return entity.position().add(0, entity.getBbHeight() * 0.5D, 0); }
    private static Vec3 clippedEnd(Cast cast, double distance) {
        Vec3 end = cast.origin.add(cast.direction.scale(distance));
        return cast.level.clip(new ClipContext(cast.origin, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, cast.user)).getLocation();
    }
    private static boolean visible(Cast cast, LivingEntity target) {
        return cast.level.clip(new ClipContext(cast.origin, center(target), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, cast.user)).getType() == HitResult.Type.MISS;
    }
    private static boolean insideBeam(Cast cast, LivingEntity target, Vec3 end, double radius) {
        return center(target).subtract(cast.origin).dot(cast.direction) >= 0
                && (target.getBoundingBox().inflate(radius).contains(cast.origin)
                || target.getBoundingBox().inflate(radius).clip(cast.origin, end).isPresent());
    }

    private static void damage(Cast cast, LivingEntity target, float physicalRatio, float magicRatio) {
        ISlashBladeState state = NamelessSeries.getBladeState(cast.user);
        if (state == null) return;
        boolean wasClicked = state.onClick();
        state.setOnClick(true);
        try {
            if (cast.user instanceof Player player && !ForgeHooks.onPlayerAttackTarget(player, target)) return;
            double base = cast.base(target);
            if (base <= 0) return;
            float before = target.getHealth();
            boolean hit = false;
            if (physicalRatio > 0) {
                target.invulnerableTime = 0;
                DamageSource physical = cast.user instanceof Player player ? cast.user.damageSources().playerAttack(player)
                        : cast.user.damageSources().mobAttack(cast.user);
                hit = target.hurt(physical, (float) (base * physicalRatio));
            }
            if (magicRatio > 0 && target.isAlive()) {
                target.invulnerableTime = 0;
                hit |= target.hurt(cast.user.damageSources().indirectMagic(cast.user, cast.user), (float) (base * magicRatio));
            }
            if (hit) {
                // One native item hook per pulse preserves kill/proud-soul/durability
                // bookkeeping. No Fire Aspect call: Caliburn stays purely physical.
                AttackHelper.handleEnchantmentsAndDurability(cast.user, target);
                if (cast.user instanceof Player player) {
                    player.awardStat(Stats.DAMAGE_DEALT, Math.round(Math.max(0, before - target.getHealth()) * 10));
                    player.causeFoodExhaustion(0.1F);
                }
            }
        } finally { state.setOnClick(wasClicked); }
    }

    private static void sound(Cast cast, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        cast.level.playSound(null, cast.user.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @SubscribeEvent public static void cleanUp(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) CASTS.values().removeIf(cast -> {
            if (cast.valid()) return false;
            if (EXCALIBUR.equals(cast.art) ? cast.completed != 7 : !cast.finished)
                HolySwordVisuals.phase(cast.user, cast.started, EXCALIBUR.equals(cast.art),
                        Stage.CANCEL, cast.origin, cast.origin);
            return true;
        });
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { CASTS.clear(); }
}
