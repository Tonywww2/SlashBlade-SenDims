package com.tonywww.slashblade_sendims.sa;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.events.SuperSlashArtsReleaseEvent;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import com.tonywww.slashblade_sendims.se.NamelessStorm;
import com.tonywww.slashblade_sendims.se.SEEventHandlers;
import com.tonywww.slashblade_sendims.utils.PositionedSlashes;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import mods.flammpfeil.slashblade.util.AttackHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Three opening strikes, a conditional bolt and a returning pursuit share one 12x cast.
 * Every strike is a native SlashBlade slash that damages all enemies where it appears;
 * only the bolt stays on the thrust target.
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class NamelessThreefold {
    public static final ResourceLocation ID = SenDims.prefix("nameless_threefold");
    public static final ResourceLocation PURSUIT_ID = SenDims.prefix("nameless_threefold_pursuit");
    public static final int OPENING_TICKS = 34;
    public static final int FINISH_TICKS = 64;
    private static final float[] RATIOS = {3.0F, 3.0F, 1.0F, 1.0F, 4.0F};
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.72F, 0.12F), 1.1F);
    private static final Map<UUID, Cast> CASTS = new HashMap<>();

    private NamelessThreefold() {}

    private static final class Cast {
        final LivingEntity user;
        final ServerLevel level;
        final ItemStack blade;
        final long started;
        final Map<UUID, Double> baseDamage = new HashMap<>();
        int completed;
        int pursuitVisuals;
        boolean pursuitInitialized;
        boolean pursuing;
        UUID thrustTarget;
        final PositionedSlashes.Group thrust = new PositionedSlashes.Group();
        Vec3 thrustStart;

        Cast(LivingEntity user, ServerLevel level) {
            this.user = user;
            this.level = level;
            this.blade = user.getMainHandItem();
            this.started = level.getGameTime();
        }

        double base(LivingEntity target) {
            // Cache the native 1x calculation per target. Later stages cannot gain an
            // extra multiplier from falling, attack cooldowns or repeated rank changes.
            return baseDamage.computeIfAbsent(target.getUUID(), id ->
                    Math.max(0.0D, AttackHelper.calculateTotalDamage(user, target, 1.0F, false)));
        }

        boolean valid() {
            ISlashBladeState state = NamelessSeries.getBladeState(user);
            return user.isAlive() && !user.isRemoved() && user.level() == level
                    && user.getMainHandItem() == blade && state != null
                    && (ID.equals(state.getComboSeq()) || PURSUIT_ID.equals(state.getComboSeq()))
                    && level.getGameTime() - started <= FINISH_TICKS;
        }
    }

    public static boolean isCombo(ResourceLocation combo) {
        return ID.equals(combo);
    }

    private static boolean usesThisArt(ServerPlayer player) {
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        return state != null && ID.equals(state.getSlashArtsKey());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void checkRelease(SlashBladeEvent.PerformSlashArtEvent event) {
        if (event.getEntityLiving().level().isClientSide()) return;
        Cast cast = CASTS.get(event.getEntityLiving().getUUID());
        if (isCombo(event.getComboState()) && event.getType() != SlashArts.ArtsType.Fail
                && cast != null && cast.valid()) {
            // Reject duplicate entry into the running animation before AP is spent.
            // Once it ends, another cast can begin immediately; no item cooldown is added.
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void acceptedRelease(SlashBladeEvent.PerformSlashArtEvent event) {
        if (isCombo(event.getComboState()) && event.getType() != SlashArts.ArtsType.Fail) {
            begin(event.getEntityLiving());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void checkSuperRelease(SuperSlashArtsReleaseEvent event) {
        ServerPlayer player = event.getPlayer();
        if (!usesThisArt(player)) return;
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        ItemStack blade = player.getMainHandItem();
        ComboState current = ComboStateRegistry.REGISTRY.get().getValue(state.resolvCurrentComboStateTicks(player).getValue());
        var types = SwordType.from(blade);
        if (!player.onGround()
                || state.isBroken() || state.isSealed() || state.getDamage() > 0
                || !types.contains(SwordType.BEWITCHED) || !types.contains(SwordType.FIERCEREDGE)
                || current == null || current.getPriority() <= 50) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void acceptedSuperRelease(SuperSlashArtsReleaseEvent event) {
        // Native SSA selects the combo directly and does not emit PerformSlashArtEvent.
        if (usesThisArt(event.getPlayer())) begin(event.getPlayer());
    }

    private static void begin(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel level)
                || !(user.getMainHandItem().getItem() instanceof ItemSlashBlade)) return;
        Cast existing = CASTS.get(user.getUUID());
        if (existing != null && existing.valid()) return;
        ISlashBladeState state = NamelessSeries.getBladeState(user);
        if (state == null) return;
        CASTS.put(user.getUUID(), new Cast(user, level));
        if (user instanceof ServerPlayer player) {
            // Run once, after the pack's cancellable AP check, for normal and super SA.
            if (SEEventHandlers.isSEActive(state, player.experienceLevel, SBSDSpecialEffects.NAMELESS_SE_STORM)) {
                NamelessStorm.onChargeAction(player, state);
            }
        }
    }

    /** Keep the passive bolt in the same damage budget, even with a low global blade scale. */
    public static float limitStormDamage(ServerPlayer user, LivingEntity target, float damage) {
        Cast cast = CASTS.get(user.getUUID());
        return cast == null ? damage : (float) Math.min(damage, cast.base(target) * NamelessSeries.STORM_LIGHTNING_DAMAGE_RATIO);
    }

    public static void tick(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel)) return;
        Cast cast = CASTS.get(user.getUUID());
        // Non-player users can enter the registered combo without a release event.
        if (cast == null && !(user instanceof Player) && ComboState.getElapsed(user) == 0) {
            begin(user);
            cast = CASTS.get(user.getUUID());
        }
        if (cast == null || !cast.valid()) return;
        long elapsed = ComboState.getElapsed(user);
        if (elapsed == 18 || elapsed == 20) {
            int bit = 1 << (elapsed == 18 ? 5 : 6);
            if ((cast.completed & bit) == 0) {
                cast.completed |= bit;
                ring(cast, center(user), elapsed == 18 ? 0.7D : 1.0D, 12);
                if (elapsed == 18) cast.level.playSound(null, user.blockPosition(),
                        SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8F, 0.7F);
            }
        }
        int phase = elapsed == 4 ? 0 : elapsed == 10 ? 1 : elapsed == 22 ? 2 : elapsed == 28 ? 3 : -1;
        if (phase < 0 || (cast.completed & (1 << phase)) != 0) return;
        cast.completed |= 1 << phase;
        if (phase < 2) sweep(cast, phase);
        else if (phase == 2) thrust(cast);
        else lightning(cast);
    }

    private static boolean enemy(LivingEntity user, LivingEntity target) {
        return target != user && target.isAlive() && !target.isSpectator()
                && !user.isAlliedTo(target) && target.isAttackable() && !target.skipAttackInteraction(user)
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }

    private static Vec3 forward(LivingEntity user) {
        Vec3 look = user.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        return look.lengthSqr() < 1.0E-6D ? Vec3.directionFromRotation(0.0F, user.getYRot()) : look.normalize();
    }

    private static Vec3 center(Entity entity) {
        return entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
    }

    private static void sweep(Cast cast, int phase) {
        Vec3 origin = center(cast.user);
        Vec3 forward = forward(cast.user);
        Vec3 slashPos = origin.add(forward.scale(0.7D));
        float roll = phase == 0 ? 20.0F : -25.0F;
        slashVisual(cast, slashPos, roll, phase == 0 ? 0xEEF5FF : 0xFFD166, 2.1F, 0.0F);
        // The larger crescent carries the damage, so its whole drawn area hits.
        strike(cast, slashPos, roll + 8.0F, 0xFFF6CF, 2.4F, 15.0F, RATIOS[phase]).spawn();
        cast.level.playSound(null, cast.user.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.2F, phase == 0 ? 0.65F : 0.8F);
        cast.level.sendParticles(phase == 0 ? ParticleTypes.CLOUD : GOLD, origin.x, origin.y, origin.z,
                24, 1.1D, 0.25D, 1.1D, 0.04D);
        cast.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, slashPos.x, slashPos.y, slashPos.z,
                20, 0.9D, 0.35D, 0.9D, 0.04D);
    }

    private static void thrust(Cast cast) {
        LivingEntity user = cast.user;
        Vec3 direction = forward(user);
        Vec3 start = center(user);
        cast.thrustStart = start;
        // Vanilla collision clipping preserves walls, entity collisions and the world border.
        move(cast, direction.scale(6.0D));
        Vec3 end = center(user).add(direction.scale(1.25D));
        // Hidden slashes along the dash keep the pierce; all share one hit list, so every
        // enemy on the path or inside the closing crescents takes the 5x strike once.
        int steps = Math.max(1, (int) Math.ceil(start.distanceTo(end) / 1.5D));
        for (int i = 0; i <= steps; i++) {
            PositionedSlashes.at(user, start.lerp(end, i / (double) steps)).hidden().size(0.5F).radius(1.5D)
                    .lifetime(8).ratio(RATIOS[2]).group(cast.thrust).spawn();
        }
        Vec3 slashPos = center(user).add(direction.scale(0.5D));
        slashVisual(cast, slashPos, 90.0F, 0xFFD166, 2.4F, 0.0F);
        strike(cast, slashPos, 85.0F, 0xFFF6CF, 2.8F, 12.0F, RATIOS[2]).group(cast.thrust).spawn();
        cast.level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.65F);
        trail(cast, start, center(user));
        cast.level.sendParticles(ParticleTypes.FLASH, user.getX(), user.getY() + 1.0D, user.getZ(),
                1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** The locked target when the thrust hit it, otherwise the damaged enemy nearest the dash start. */
    private static LivingEntity thrustTarget(Cast cast) {
        if (cast.thrustTarget != null) {
            return cast.level.getEntity(cast.thrustTarget) instanceof LivingEntity target ? target : null;
        }
        List<LivingEntity> damaged = cast.thrust.damaged();
        if (damaged.isEmpty() || cast.thrustStart == null) return null;
        Entity locked = NamelessSeries.getBladeState(cast.user).getTargetEntity(cast.level);
        LivingEntity target = locked instanceof LivingEntity living && damaged.contains(living) ? living
                : damaged.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(cast.thrustStart))).orElse(null);
        if (target != null) cast.thrustTarget = target.getUUID();
        return target;
    }

    private static void move(Cast cast, Vec3 offset) {
        LivingEntity user = cast.user;
        user.move(MoverType.SELF, offset);
        user.setDeltaMovement(user.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
        user.hurtMarked = true;
        if (user instanceof ServerPlayer player) {
            player.connection.teleport(user.getX(), user.getY(), user.getZ(), user.getYRot(), user.getXRot());
        }
    }

    private static void trail(Cast cast, Vec3 start, Vec3 end) {
        int points = Math.max(1, Math.min(36, (int) Math.ceil(start.distanceTo(end) * 5.0D)));
        for (int i = 0; i <= points; i++) {
            Vec3 point = start.lerp(end, i / (double) points);
            cast.level.sendParticles(GOLD, point.x, point.y, point.z, 2, 0.06D, 0.06D, 0.06D, 0.0D);
            cast.level.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z,
                    1, 0.03D, 0.03D, 0.03D, 0.01D);
        }
    }

    /** Automatic returning attack, inspired by Last Smith's rotating third slash. */
    public static void beginPursuit(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel)) return;
        Cast cast = CASTS.get(user.getUUID());
        if (cast == null || !cast.valid() || cast.pursuitInitialized) return;
        cast.pursuitInitialized = true;
        LivingEntity target = thrustTarget(cast);
        if (target == null || !enemy(user, target)
                || target.distanceToSqr(user) > 64.0D || !user.hasLineOfSight(target)) return;
        cast.pursuing = true;
        Vec3 start = center(user);
        Vec3 offset = target.position().subtract(user.position()).multiply(1.0D, 0.0D, 1.0D);
        double distance = offset.length();
        if (distance > 1.2D) move(cast, offset.normalize().scale(Math.min(6.0D, distance - 1.2D)));
        trail(cast, start, center(user));
        ring(cast, center(user), 2.8D, 24);
        cast.level.playSound(null, user.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3,
                SoundSource.PLAYERS, 1.1F, 0.75F);
    }

    public static void pursuitTick(LivingEntity user) {
        if (!(user.level() instanceof ServerLevel)) return;
        Cast cast = CASTS.get(user.getUUID());
        if (cast == null || !cast.valid()) return;
        long elapsed = ComboState.getElapsed(user);
        if (elapsed != 2 && elapsed != 4 && elapsed != 6) return;
        int bit = 1 << (int) elapsed;
        if ((cast.pursuitVisuals & bit) != 0) return;
        cast.pursuitVisuals |= bit;
        Vec3 origin = center(user);
        float roll = elapsed == 2 ? 35.0F : elapsed == 4 ? -25.0F : 30.0F;
        slashVisual(cast, origin, roll, 0xFFD166, 2.8F, (float) elapsed * 60.0F);
        if (elapsed != 4) return;
        if (cast.pursuing && (cast.completed & (1 << 4)) == 0) {
            cast.completed |= 1 << 4;
            // The widest returning crescent hits every enemy inside it.
            strike(cast, origin, -roll, 0xFFF6CF, 3.15F, 180.0F, RATIOS[4]).spawn();
        } else {
            slashVisual(cast, origin, -roll, 0xFFF6CF, 3.15F, 180.0F);
        }
        ring(cast, origin, 3.6D, 32);
        cast.level.sendParticles(ParticleTypes.FLASH, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
        cast.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                36, 1.6D, 0.5D, 1.6D, 0.06D);
        cast.level.playSound(null, user.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.4F, 0.65F);
        cast.level.playSound(null, user.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                SoundSource.WEATHER, 0.8F, 1.2F);
    }

    private static void ring(Cast cast, Vec3 origin, double radius, int points) {
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0D * i / points;
            cast.level.sendParticles(GOLD, origin.x + Math.cos(angle) * radius,
                    origin.y - 0.35D, origin.z + Math.sin(angle) * radius, 1, 0.03D, 0.05D, 0.03D, 0);
        }
    }

    private static void lightning(Cast cast) {
        LivingEntity target = thrustTarget(cast);
        if (target == null || !enemy(cast.user, target) || target.distanceToSqr(cast.user) > 256.0D) return;
        float damage = (float) (cast.base(target) * RATIOS[3]);
        if (damage <= 0.0F) return;
        NamelessSeries.spawnLightningVisual(cast.level, target.position());
        cast.level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 0.7F, 1.3F);
        target.invulnerableTime = 0;
        target.hurt(new DamageSource(cast.level.damageSources().lightningBolt().typeHolder(), cast.user), damage);
        cast.level.sendParticles(GOLD, target.getX(), target.getY() + 1.0D, target.getZ(),
                48, 0.5D, 1.2D, 0.5D, 0.05D);
        Vec3 origin = center(target);
        ring(cast, origin, 2.4D, 24);
        cast.level.sendParticles(ParticleTypes.EXPLOSION, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
        cast.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, origin.x, origin.y, origin.z,
                32, 0.8D, 1.2D, 0.8D, 0.08D);
    }

    /** A visible native slash that deals native melee damage to every enemy inside its crescent. */
    private static PositionedSlashes.Builder strike(Cast cast, Vec3 position, float roll, int color, float size,
                                                    float offset, float ratio) {
        return PositionedSlashes.at(cast.user, position).roll(roll).offset(offset).color(color).size(size)
                .rank(IConcentrationRank.ConcentrationRanks.S.level).lifetime(8).ratio(ratio);
    }

    private static void slashVisual(Cast cast, Vec3 position, float roll, int color, float size, float offset) {
        EntitySlashEffect visual = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, cast.level);
        visual.setPos(position);
        visual.setYRot(cast.user.getYRot());
        visual.yRotO = visual.getYRot();
        visual.setRotationRoll(roll);
        visual.setRotationOffset(offset);
        visual.setColor(color);
        // Below C rank the native renderer overwrites the chosen color with gray.
        // A fixed visual S rank enables the gold/white layers without changing damage.
        visual.setRank(IConcentrationRank.ConcentrationRanks.S.level);
        visual.setIsCritical(true);
        visual.setBaseSize(size);
        visual.setLifetime(8);
        visual.setDamage(0.0D);
        visual.setMute(true);
        // Intentionally ownerless: native slash entities with an owner cause another area attack,
        // even at zero damage, because they can add rank bonus. This entity is visual only;
        // strike() makes the damaging slashes.
        cast.level.addFreshEntity(visual);
    }

    @SubscribeEvent
    public static void cleanUp(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) CASTS.values().removeIf(cast -> !cast.valid());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        CASTS.clear();
    }
}
