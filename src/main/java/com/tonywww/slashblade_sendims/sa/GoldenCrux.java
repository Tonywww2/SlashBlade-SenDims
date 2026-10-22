package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.GoldenCruxVisualPacket;
import com.tonywww.slashblade_sendims.network.GoldenCruxVisualPacket.Stage;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import mods.flammpfeil.slashblade.SlashBlade.RegistryEvents;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Both arts share the grounded crown; EX retains its native slash damage carriers. */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class GoldenCrux {
    public static final ResourceLocation ART = SenDims.prefix("golden_crux");
    public static final ResourceLocation DIVE = SenDims.prefix("golden_crux_dive");
    public static final ResourceLocation LANDING = SenDims.prefix("golden_crux_landing");
    public static final int LEAP_TICKS = 10;
    // Native slash RGB ignores this byte, as with the existing yakuman visual marker.
    public static final int HIDDEN_SLASH_MARKER = 0x47000000;
    private static final int MAX_TICKS = 40, BURST_DELAY = 3;
    private static final double SEARCH_RADIUS = 12.0, RING_RADIUS = 4.0;
    // Preserve the old ordinary art's nominal 11*0.2 + 5*0.3 damage budget.
    private static final float PLUNGE_DAMAGE = 2.2F, BARB_DAMAGE = 1.5F;
    private static final Map<UUID, Cast> CASTS = new HashMap<>();

    private GoldenCrux() {}

    private static final class Cast {
        final LivingEntity user;
        final ServerLevel level;
        final ItemStack blade;
        final LivingEntity target;
        final Vec3 aim;
        final long started;
        final Map<UUID, Double> damage = new HashMap<>();
        Vec3 previous, landing;
        long landedAt = -1;
        boolean diving, stabbed, burst;

        Cast(LivingEntity user, ServerLevel level, LivingEntity target, Vec3 aim) {
            this.user = user; this.level = level; this.target = target; this.aim = aim;
            blade = user.getMainHandItem(); started = level.getGameTime(); previous = user.position();
        }

        boolean valid() {
            ISlashBladeState state = NamelessSeries.getBladeState(user);
            return user.isAlive() && !user.isRemoved() && user.level() == level && !user.isPassenger()
                    && user.getMainHandItem() == blade && state != null && !state.isBroken() && !state.isSealed()
                    && isCombo(state.getComboSeq()) && level.getGameTime() - started <= MAX_TICKS;
        }

        double base(LivingEntity target) {
            return damage.computeIfAbsent(target.getUUID(), id -> Math.max(0,
                    AttackHelper.calculateTotalDamage(user, target, 1.0F, false)));
        }
    }

    private static boolean isCombo(ResourceLocation id) {
        return ART.equals(id) || DIVE.equals(id) || LANDING.equals(id);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void guardRelease(SlashBladeEvent.PerformSlashArtEvent event) {
        LivingEntity user = event.getEntityLiving();
        if (user.level().isClientSide() || !ART.equals(event.getComboState())
                || event.getType() == SlashArts.ArtsType.Fail) return;
        if (user.isPassenger() || CASTS.containsKey(user.getUUID())) event.setCanceled(true);
    }

    /** Native combo entry occurs only after the pack's AP checks have succeeded. */
    public static void begin(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel level) || user.isPassenger()
                || !(user.getMainHandItem().getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = NamelessSeries.getBladeState(user);
        if (state == null) return;
        Cast old = CASTS.get(user.getUUID());
        if (old != null && old.valid()) return;
        Entity locked = state.getTargetEntity(level);
        LivingEntity target = locked instanceof LivingEntity living && enemy(user, living)
                && user.distanceToSqr(living) <= SEARCH_RADIUS * SEARCH_RADIUS && user.hasLineOfSight(living)
                ? living : nearest(user, level);
        Vec3 direction = forward(user);
        Vec3 aim = target == null ? user.position().add(direction.scale(4)) : target.position();
        Vec3 flat = aim.subtract(user.position()).multiply(1, 0, 1);
        // The leap covers the approach; retain a little separation from the target's body.
        if (flat.lengthSqr() > 0.25) flat = flat.subtract(flat.normalize().scale(0.45));
        Vec3 horizontal = limit(flat.scale(1.0 / 6.8), 1.7);
        Cast cast = new Cast(user, level, target, aim);
        CASTS.put(user.getUUID(), cast);
        user.fallDistance = 0;
        user.setOnGround(false);
        motion(user, new Vec3(horizontal.x, 0.82, horizontal.z));
        GoldenCruxVisualPacket.send(user, cast.started, Stage.LEAP, user.position(), horizontal);
        level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1, SoundSource.PLAYERS, 0.8F, 0.85F);
    }

    private static LivingEntity nearest(LivingEntity user, ServerLevel level) {
        Vec3 direction = forward(user);
        return level.getEntitiesOfClass(LivingEntity.class, user.getBoundingBox().inflate(SEARCH_RADIUS),
                other -> enemy(user, other) && user.distanceToSqr(other) <= SEARCH_RADIUS * SEARCH_RADIUS
                        && other.position().subtract(user.position()).dot(direction) >= 0 && user.hasLineOfSight(other))
                .stream().min(Comparator.comparingDouble(user::distanceToSqr)).orElse(null);
    }

    private static boolean enemy(LivingEntity user, LivingEntity target) {
        return target != user && target.isAlive() && target.isAttackable() && !target.isSpectator()
                && !user.isAlliedTo(target) && !target.skipAttackInteraction(user)
                && (!(target instanceof Player player) || !player.isCreative() && !player.isSpectator());
    }

    private static Vec3 forward(LivingEntity user) {
        Vec3 flat = user.getLookAngle().multiply(1, 0, 1);
        return flat.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0, user.getYRot()) : flat.normalize();
    }

    private static Vec3 limit(Vec3 value, double maximum) {
        return value.lengthSqr() > maximum * maximum ? value.normalize().scale(maximum) : value;
    }

    private static void motion(LivingEntity user, Vec3 velocity) {
        // Vanilla velocity packets drive the local player too. No per-frame teleport corrections.
        user.setDeltaMovement(velocity);
        user.hurtMarked = true;
    }

    private static void dive(Cast cast) {
        cast.diving = true;
        Vec3 aim = cast.target != null && enemy(cast.user, cast.target)
                && cast.user.distanceToSqr(cast.target) < 256 ? cast.target.position() : cast.aim;
        Vec3 flat = aim.subtract(cast.user.position()).multiply(1, 0, 1);
        if (flat.lengthSqr() > 0.25) flat = flat.subtract(flat.normalize().scale(0.4));
        Vec3 horizontal = limit(flat.scale(0.3), 0.9);
        NamelessSeries.getBladeState(cast.user).updateComboSeq(cast.user, DIVE);
        motion(cast.user, new Vec3(horizontal.x, -1.65, horizontal.z));
        GoldenCruxVisualPacket.send(cast.user, cast.started, Stage.DIVE, cast.user.position(), horizontal);
        cast.level.playSound(null, cast.user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.65F);
    }

    private static void stab(Cast cast, boolean landing) {
        if (cast.stabbed) return;
        Vec3 start = cast.previous.add(0, 0.4, 0), end = cast.user.position().add(0, 0.4, 0);
        AABB region = new AABB(start, end).inflate(landing ? 1.4 : 0.75, 1.2, landing ? 1.4 : 0.75);
        var targets = cast.level.getEntitiesOfClass(LivingEntity.class, region,
                other -> enemy(cast.user, other) && cast.user.hasLineOfSight(other)
                        && (landing || other.getBoundingBox().inflate(0.75).clip(start, end).isPresent()
                        || other.getBoundingBox().inflate(0.75).contains(end)));
        LivingEntity target = targets.contains(cast.target) ? cast.target : targets.stream()
                .min(Comparator.comparingDouble(cast.user::distanceToSqr)).orElse(null);
        if (target != null) {
            cast.stabbed = true;
            hit(cast, target, PLUNGE_DAMAGE);
        }
    }

    private static void land(Cast cast) {
        cast.landedAt = cast.level.getGameTime();
        cast.landing = cast.user.position();
        stab(cast, true);
        cast.user.fallDistance = 0;
        motion(cast.user, Vec3.ZERO);
        NamelessSeries.getBladeState(cast.user).updateComboSeq(cast.user, LANDING);
        GoldenCruxVisualPacket.send(cast.user, cast.started, Stage.STAB, cast.landing, Vec3.ZERO);
        cast.level.playSound(null, cast.user.blockPosition(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.3F, 0.6F);
    }

    private static void burst(Cast cast) {
        cast.burst = true;
        // Each enemy is processed once for the entire crown, never once per rendered barb.
        for (LivingEntity target : cast.level.getEntitiesOfClass(LivingEntity.class,
                new AABB(cast.landing, cast.landing).inflate(RING_RADIUS, 3.5, RING_RADIUS),
                other -> enemy(cast.user, other))) {
            if (target.position().subtract(cast.landing).horizontalDistanceSqr() <= RING_RADIUS * RING_RADIUS
                    && Math.abs(target.getY() - cast.landing.y) <= 3.5 && cast.user.hasLineOfSight(target)) {
                hit(cast, target, BARB_DAMAGE);
            }
        }
        showCrown(cast.user, cast.started, cast.landing);
    }

    /** Visual-only entry shared by normal landing and each EX strike. */
    public static void showCrown(LivingEntity user, long castId, Vec3 origin) {
        if (!(user.level() instanceof ServerLevel level)) return;
        GoldenCruxVisualPacket.send(user, castId, Stage.BURST, origin, Vec3.ZERO);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.EVOKER_FANGS_ATTACK,
                SoundSource.PLAYERS, 1.3F, 0.65F);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.BLAZE_SHOOT,
                SoundSource.PLAYERS, 0.8F, 0.7F);
    }

    private static void hit(Cast cast, LivingEntity target, float ratio) {
        ISlashBladeState state = NamelessSeries.getBladeState(cast.user);
        if (state == null) return;
        boolean clicked = state.onClick();
        state.setOnClick(true);
        try {
            if (cast.user instanceof Player player && !ForgeHooks.onPlayerAttackTarget(player, target)) return;
            float amount = (float) (cast.base(target) * ratio);
            if (amount <= 0) return;
            AttackHelper.FireAspectResult fire = AttackHelper.handleFireAspect(cast.user, target);
            DamageSource source = cast.user instanceof Player player ? cast.user.damageSources().playerAttack(player)
                    : cast.user.damageSources().mobAttack(cast.user);
            target.invulnerableTime = 0;
            if (!target.hurt(source, amount)) {
                AttackHelper.handleFailedAttack(cast.user, target, fire);
                return;
            }
            AttackHelper.playAttackEffects(cast.user, target, false);
            // The native HitEvent triggers the equipped blade's frenzy and other SE once.
            AttackHelper.handleEnchantmentsAndDurability(cast.user, target);
            AttackHelper.handlePostAttackEffects(cast.user, target, fire);
            StunManager.setStun(target, 15);
        } finally {
            state.setOnClick(clicked);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || CASTS.isEmpty()) return;
        Iterator<Cast> iterator = CASTS.values().iterator();
        while (iterator.hasNext()) {
            Cast cast = iterator.next();
            if (!cast.valid()) {
                GoldenCruxVisualPacket.send(cast.user, cast.started, Stage.CANCEL, cast.user.position(), Vec3.ZERO);
                iterator.remove();
                continue;
            }
            long age = cast.level.getGameTime() - cast.started;
            cast.user.fallDistance = 0;
            if (cast.landedAt >= 0) {
                long recovery = cast.level.getGameTime() - cast.landedAt;
                if (!cast.burst && recovery >= BURST_DELAY) burst(cast);
                if (recovery >= 8) iterator.remove();
            } else {
                if (!cast.diving && age >= LEAP_TICKS) dive(cast);
                if (cast.diving && age > LEAP_TICKS) {
                    stab(cast, false);
                    // Ground contact is reported by vanilla movement, including slabs/stairs.
                    if (cast.user.onGround()) land(cast);
                }
            }
            cast.previous = cast.user.position();
        }
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent event) {
        if (CASTS.containsKey(event.getEntity().getUUID())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        CASTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { CASTS.clear(); }

    public static void doGoldenCruxImpactSmall(LivingEntity attacker) {
        doGoldenCruxImpact(attacker, 6, 3);
    }

    public static void doGoldenCruxImpact(LivingEntity attacker, int count1, int count2) {
        if (!(attacker.level() instanceof ServerLevel level)) return;

        for (int i = 0; i < count1; i++) {
            float angle = i * (360f / count1);

            EntitySlashEffect slashEffect = new EntitySlashEffect(RegistryEvents.SlashEffect, level);

            slashEffect.setPos(
                    attacker.getX(),
                    attacker.getY() - 0.75f,
                    attacker.getZ()
            );

            slashEffect.setDamage(0.2f);
            slashEffect.setRank(7);
            slashEffect.setBaseSize(1.75f);

            slashEffect.setYRot(angle);
            slashEffect.setXRot(0f);

            slashEffect.setRotationRoll(90.0F);
            slashEffect.setColor(HIDDEN_SLASH_MARKER | 0xffff00);
            slashEffect.setSilent(true);

            slashEffect.setOwner(attacker);

            level.addFreshEntity(slashEffect);
        }

        // 小刺
        for (int i = 0; i < count2; i++) {
            float angle = i * (360f / count2);

            EntitySlashEffect slashEffect = new EntitySlashEffect(RegistryEvents.SlashEffect, level);

            slashEffect.setPos(
                    attacker.getX(),
                    attacker.getY() - 0.5f,
                    attacker.getZ()
            );

            slashEffect.setDamage(0.3f);
            slashEffect.setRank(7);
            slashEffect.setBaseSize(1f);

            slashEffect.setYRot(angle);
            slashEffect.setXRot(0f);

            slashEffect.setRotationRoll(90.0F);
            slashEffect.setColor(HIDDEN_SLASH_MARKER | 0xFFD700);
            slashEffect.setSilent(true);

            slashEffect.setOwner(attacker);

            level.addFreshEntity(slashEffect);
        }

    }
}
