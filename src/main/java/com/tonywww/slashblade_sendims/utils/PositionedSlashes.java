package com.tonywww.slashblade_sendims.utils;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import com.tonywww.slashblade_sendims.sa.GoldenCrux;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.entity.EntitySlashEffect;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Native SlashBlade slash entities whose area damage is centred on the slash itself.
 *
 * <p>A native owned slash attacks around its owner. The slashes made here are marked indirect,
 * and {@code YakumanSlashMixin} sends their periodic area attack to {@link #damage}: every enemy
 * within {@code radius} of the slash entity takes {@link AttackManager#doMeleeAttack} with the
 * slash's damage as the combo ratio. The native melee formula therefore applies in full
 * (rank, enchantments, SE hit hooks, proud souls, durability), and the slash's own
 * {@code alreadyHits} list keeps each enemy to one hit per slash.</p>
 *
 * <p>DoSlashEvent is intentionally not posted: listeners such as ammo arts and Frenzied Flame
 * treat that event as an ordinary swing.</p>
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class PositionedSlashes {
    public static final String TAG = "sbsd.positioned_slash";
    /** High colour byte that cancels native rendering: damage carrier only, art draws its own visual. */
    public static final int HIDDEN = GoldenCrux.HIDDEN_SLASH_MARKER;
    /** Native slash.obj has radius 100; the renderer scales it by baseSize * 0.03 to 0.0375. */
    public static final double VISUAL_RADIUS_PER_SIZE = 3.0D;
    private static final Map<EntitySlashEffect, Area> AREAS = new IdentityHashMap<>();

    private PositionedSlashes() {}

    /** Wraps damage so integrations can tell it is secondary SA damage. */
    @FunctionalInterface
    public interface Scope { void run(Runnable action); }

    /** Several slashes of one strike share it, so each enemy is hit once per strike. */
    public static final class Group {
        private final Set<UUID> hit = new HashSet<>();
        private final List<LivingEntity> order = new ArrayList<>();
        public boolean contains(Entity entity) { return hit.contains(entity.getUUID()); }
        /** Enemies the strike actually damaged, in hit order. */
        public List<LivingEntity> damaged() { return order; }
    }

    private record Area(LivingEntity user, ItemStack blade, double radius, Group group,
                        Consumer<LivingEntity> onHit, Scope scope, BooleanSupplier valid) {}

    public static Builder at(LivingEntity user, Vec3 position) { return new Builder(user, position); }

    public static boolean isPositioned(EntitySlashEffect slash) {
        return slash.getPersistentData().getBoolean(TAG);
    }

    public static final class Builder {
        private final LivingEntity user;
        private final Vec3 position;
        private float yaw, roll, offset, size = 1.0F;
        private float rank = -1.0F;
        private int color = 0x3333FF, lifetime = 10;
        private double ratio, radius = -1.0D;
        private boolean critical, mute = true;
        private KnockBacks knockBack = KnockBacks.cancel;
        private Group group;
        private Consumer<LivingEntity> onHit;
        private Scope scope = Runnable::run;
        private BooleanSupplier valid = () -> true;
        private final List<String> tags = new ArrayList<>();

        private Builder(LivingEntity user, Vec3 position) {
            this.user = user;
            this.position = position;
            this.yaw = user.getYRot();
        }

        public Builder yaw(float value) { yaw = value; return this; }
        public Builder roll(float value) { roll = value; return this; }
        public Builder offset(float value) { offset = value; return this; }
        public Builder size(float value) { size = value; return this; }
        /** Visual rank only; below C the native renderer draws the slash gray. */
        public Builder rank(float value) { rank = value; return this; }
        public Builder color(int value) { color = value; return this; }
        public Builder hidden() { color = HIDDEN | (color & 0xFFFFFF); return this; }
        public Builder lifetime(int value) { lifetime = value; return this; }
        /** Native combo ratio passed to doMeleeAttack. */
        public Builder ratio(double value) { ratio = value; return this; }
        /** Hit radius around the slash; defaults to its drawn radius. */
        public Builder radius(double value) { radius = value; return this; }
        public Builder critical(boolean value) { critical = value; return this; }
        public Builder mute(boolean value) { mute = value; return this; }
        public Builder knockBack(KnockBacks value) { knockBack = value; return this; }
        public Builder group(Group value) { group = value; return this; }
        public Builder onHit(Consumer<LivingEntity> value) { onHit = value; return this; }
        public Builder scope(Scope value) { scope = value; return this; }
        /** Checked before each area attack; a cancelled art stops dealing damage. */
        public Builder valid(BooleanSupplier value) { valid = value; return this; }
        public Builder tag(String key) { tags.add(key); return this; }

        public EntitySlashEffect spawn() {
            if (!(user.level() instanceof ServerLevel level)) return null;
            EntitySlashEffect slash = new EntitySlashEffect(SlashBlade.RegistryEvents.SlashEffect, level);
            slash.setPos(position);
            slash.setOwner(user);
            // Indirect selects the slash-centred area attack redirected by YakumanSlashMixin.
            slash.setIndirect(true);
            slash.setCycleHit(false);
            slash.setYRot(yaw);
            slash.yRotO = yaw;
            slash.setXRot(0.0F);
            slash.setRotationRoll(roll);
            slash.setRotationOffset(offset);
            slash.setColor(color);
            if (rank >= 0) slash.setRank(rank);
            slash.setBaseSize(size);
            slash.setLifetime(lifetime);
            slash.setDamage(ratio);
            slash.setIsCritical(critical);
            slash.setMute(mute);
            slash.setKnockBack(knockBack);
            slash.getPersistentData().putBoolean(TAG, true);
            for (String key : tags) slash.getPersistentData().putBoolean(key, true);
            double reach = radius >= 0 ? radius : size * VISUAL_RADIUS_PER_SIZE;
            AREAS.put(slash, new Area(user, user.getMainHandItem(), reach, group, onHit, scope, valid));
            level.addFreshEntity(slash);
            return slash;
        }
    }

    /** Called from the native slash tick (every second tick of its lifetime). */
    public static List<Entity> damage(EntitySlashEffect slash, Consumer<LivingEntity> beforeHit,
                                      boolean forceHit, boolean resetHit, List<Entity> excluded) {
        Area area = AREAS.get(slash);
        // A slash restored from disk has lost its owner context and stays harmless.
        if (area == null || !(slash.level() instanceof ServerLevel level)) return List.of();
        LivingEntity user = area.user;
        if (!user.isAlive() || user.isRemoved() || user.level() != level
                || user.getMainHandItem() != area.blade || !area.valid.getAsBoolean()) return List.of();
        float ratio = (float) (slash.getDamage() * (slash.getIsCritical() ? 1.1D : 1.0D));
        if (!(ratio > 0)) return List.of();
        List<Entity> targets = targets(level, user, slash.position(), area.radius, excluded, area.group);
        if (targets.isEmpty()) return targets;
        area.scope.run(() -> {
            for (Entity entity : targets) {
                LivingEntity living = living(entity);
                if (living == null) {
                    if (area.group != null) area.group.hit.add(entity.getUUID());
                    AttackManager.doMeleeAttack(user, entity, forceHit, resetHit, ratio);
                    continue;
                }
                if (area.group != null) area.group.hit.add(living.getUUID());
                float health = living.getHealth();
                if (entity instanceof LivingEntity target) beforeHit.accept(target);
                AttackManager.doMeleeAttack(user, entity, forceHit, resetHit, ratio);
                if (living.getHealth() < health || !living.isAlive()
                        || living.getLastHurtByMob() == user && living.getLastHurtByMobTimestamp() == living.tickCount) {
                    if (area.group != null) area.group.order.add(living);
                    if (area.onHit != null) area.onHit.accept(living);
                }
            }
        });
        return targets;
    }

    /** Same filter as native area attacks, measured from the slash to each body's nearest point. */
    private static List<Entity> targets(ServerLevel level, LivingEntity user, Vec3 point, double radius,
                                        List<Entity> excluded, Group group) {
        List<Entity> found = new ArrayList<>();
        Set<LivingEntity> bodies = new HashSet<>();
        double reachSqr = radius * radius;
        for (Entity entity : level.getEntities(user, new AABB(point, point).inflate(radius + 1.0D))) {
            if (!entity.isAlive() || excluded != null && excluded.contains(entity)
                    || distanceSqr(point, entity.getBoundingBox()) > reachSqr) continue;
            LivingEntity living = living(entity);
            if (living == null) {
                if (entity.getType().is(SBSDTags.EntityTypes.NON_LIVING_SLASH_TARGETS)
                        && (group == null || !group.contains(entity))) found.add(entity);
                continue;
            }
            if (living == user || bodies.contains(living) || group != null && group.contains(living)
                    || !TargetSelector.getAreaAttackPredicate(0.0D).test(user, living)) continue;
            bodies.add(living);
            found.add(entity);
        }
        return found;
    }

    private static LivingEntity living(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        return entity instanceof PartEntity<?> part && part.getParent() instanceof LivingEntity parent ? parent : null;
    }

    private static double distanceSqr(Vec3 point, AABB box) {
        double dx = Math.max(0.0D, Math.max(box.minX - point.x, point.x - box.maxX));
        double dy = Math.max(0.0D, Math.max(box.minY - point.y, point.y - box.maxY));
        double dz = Math.max(0.0D, Math.max(box.minZ - point.z, point.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }

    @SubscribeEvent
    public static void cleanUp(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !AREAS.isEmpty()) AREAS.keySet().removeIf(Entity::isRemoved);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { AREAS.clear(); }
}
