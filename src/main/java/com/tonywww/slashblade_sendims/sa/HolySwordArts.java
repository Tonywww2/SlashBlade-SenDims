package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.HolySwordVisualPacket.Stage;
import com.tonywww.slashblade_sendims.events.SuperSlashArtsReleaseEvent;
import com.tonywww.slashblade_sendims.se.HolySwordRules;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import com.tonywww.slashblade_sendims.utils.PositionedSlashes;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Caliburn: 3+10 physical. Excalibur: (2+2)+(2+2)+(3+3), physical+magic.
 * The Caliburn opening is a native slash (group damage where its crescent appears). Both beams
 * hit 2 blocks around their centreline horizontally and 6 blocks above and below it.
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class HolySwordArts {
    public static final ResourceLocation CALIBURN = SenDims.prefix("caliburn");
    public static final ResourceLocation CALIBURN_FINISH = SenDims.prefix("caliburn_finish");
    public static final ResourceLocation EXCALIBUR = SenDims.prefix("excalibur");
    public static final int CALIBURN_CHARGE_TICKS = 9;
    public static final int EXCALIBUR_CHARGE_TICKS = 18;
    public static final double BEAM_RADIUS = 2.0D;
    public static final double BEAM_HALF_HEIGHT = 6.0D;
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
        for (LivingEntity target : beamTargets(cast, end)) {
            damage(cast, target, HolySwordRules.CALIBURN_FINISH, 0.0F);
        }
        HolySwordVisuals.phase(user, cast.started, false, Stage.FINISH, cast.origin, end);
        sound(cast, SoundEvents.PLAYER_ATTACK_STRONG, 1.3F, 0.65F);
        sound(cast, SoundEvents.TRIDENT_RIPTIDE_2, 0.9F, 1.1F);
    }

    /** Hidden native slash under the drawn crescent: every enemy inside it takes the 3x cut. */
    private static void opening(Cast cast) {
        PositionedSlashes.at(cast.user, cast.origin.add(cast.direction.scale(1.5D))).hidden()
                .yaw((float) Math.toDegrees(Math.atan2(-cast.direction.x, cast.direction.z)))
                .size(1.5F).radius(4.5D).lifetime(8).ratio(HolySwordRules.CALIBURN_OPENING)
                .spawn();
    }

    private static void beamPulse(Cast cast, int phase) {
        Vec3 end = clippedEnd(cast, 36.0D);
        for (LivingEntity target : beamTargets(cast, end)) {
            damage(cast, target, HolySwordRules.excaliburPhysical(phase), HolySwordRules.excaliburMagic(phase));
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
    private static boolean visible(Cast cast, Vec3 from, LivingEntity target) {
        return cast.level.clip(new ClipContext(from, center(target), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, cast.user)).getType() == HitResult.Type.MISS;
    }

    private static List<LivingEntity> beamTargets(Cast cast, Vec3 end) {
        List<LivingEntity> targets = new ArrayList<>();
        for (LivingEntity target : cast.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(cast.origin, end).inflate(BEAM_RADIUS, BEAM_HALF_HEIGHT, BEAM_RADIUS),
                other -> enemy(cast.user, other))) {
            Vec3 contact = beamContact(cast.origin, end, cast.direction, target.getBoundingBox());
            // Seen from the caster or from the beam itself: walls and floors still block the light.
            if (contact != null && (visible(cast, contact, target) || visible(cast, cast.origin, target))) {
                targets.add(target);
            }
        }
        return targets;
    }

    /**
     * The beam point beside a body, or null when the body is outside the hit volume: within
     * {@link #BEAM_RADIUS} of the centreline horizontally and {@link #BEAM_HALF_HEIGHT} vertically.
     */
    public static Vec3 beamContact(Vec3 origin, Vec3 end, Vec3 direction, AABB box) {
        Vec3 center = box.getCenter();
        if (center.subtract(origin).dot(direction) < 0) return null;
        Vec3 line = end.subtract(origin);
        double reach = BEAM_RADIUS + (box.getXsize() + box.getZsize()) * 0.25D;
        double ax = origin.x - center.x, az = origin.z - center.z;
        double bb = line.x * line.x + line.z * line.z, ab = ax * line.x + az * line.z;
        double aa = ax * ax + az * az - reach * reach;
        double t0, t1, closest;
        if (bb < 1.0E-6D) {
            // Vertical aim: the column itself, full length.
            if (aa > 0) return null;
            t0 = 0.0D; t1 = 1.0D; closest = Mth.clamp((center.y - origin.y) / (line.y == 0 ? 1 : line.y), 0, 1);
        } else {
            double disc = ab * ab - bb * aa;
            if (disc < 0) return null;
            double root = Math.sqrt(disc);
            t0 = Math.max(0.0D, (-ab - root) / bb);
            t1 = Math.min(1.0D, (-ab + root) / bb);
            if (t0 > t1) return null;
            closest = Mth.clamp(-ab / bb, t0, t1);
        }
        double y0 = origin.y + line.y * t0, y1 = origin.y + line.y * t1;
        if (box.maxY < Math.min(y0, y1) - BEAM_HALF_HEIGHT || box.minY > Math.max(y0, y1) + BEAM_HALF_HEIGHT) return null;
        return origin.add(line.scale(closest));
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
