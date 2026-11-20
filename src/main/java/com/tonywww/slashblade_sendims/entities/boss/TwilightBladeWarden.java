package com.tonywww.slashblade_sendims.entities.boss;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * 暮刃守卫：暮色森林终焉城堡的首领。注册为 EXTERNAL Leader，出招由自身状态机决定：
 * 蓝刀不可招架，前摇显示 Leader 蓝火粒子环；红刀可招架，不显示蓝火环，按 Leader 标准在出招前
 * PARRY_TICK 打开招架窗口并显示「危」。生命损失 30%、60% 时各锁血一次，锁血后 2 秒无敌并打断出招。
 */
public class TwilightBladeWarden extends Monster {
    /** 被招架后破防（易伤）与眩晕的时长。 */
    public static final int PARRIED_TICKS = 80;
    static final int LOCK_INVULNERABLE_TICKS = 40;
    /** 锁血线：剩余生命比例，对应损失 30%、60%。 */
    private static final float[] LOCK_RATIOS = {0.7F, 0.4F};
    /** 各阶段一轮里先出的蓝刀（不可招架）次数 {最少, 最多}。 */
    private static final int[][] BLUE_RUN = {{2, 4}, {2, 3}, {1, 2}};
    /** 各阶段一轮里蓝刀之后的红刀（可招架）次数 {最少, 最多}。 */
    private static final int[][] RED_RUN = {{1, 1}, {1, 2}, {1, 2}};
    /** 各阶段两次出招之间的间隔。 */
    private static final int[] ATTACK_INTERVAL = {22, 16, 12};
    private static final BossEvent.BossBarColor[] BAR_COLORS = {
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarColor.RED};
    private static final double APPROACH_DISTANCE = 3.5D;

    private static final EntityDataAccessor<Byte> DATA_PHASE =
            SynchedEntityData.defineId(TwilightBladeWarden.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_TELEGRAPH =
            SynchedEntityData.defineId(TwilightBladeWarden.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_TELEGRAPH_START =
            SynchedEntityData.defineId(TwilightBladeWarden.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TELEGRAPH_DURATION =
            SynchedEntityData.defineId(TwilightBladeWarden.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    @Nullable
    private WardenAttacks.Attack currentAttack;
    @Nullable
    private WardenAttacks.Move lastMove;
    /** 本轮还要出的蓝刀数，出完后轮到红刀。 */
    private int bluesLeft;
    /** 本轮还要出的红刀数，出完后开始下一轮。 */
    private int redsLeft;
    private int attackCooldown;
    private int invulnerableTicks;
    private boolean phaseChanged;
    private boolean allowLethal;
    private long telegraphEnd;
    private long redWindowAt;
    private boolean redWindowOpened;
    @Nullable
    private Vec3 lastTargetPosition;
    private int repathCooldown;

    public TwilightBladeWarden(EntityType<? extends TwilightBladeWarden> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 200;
        this.attackCooldown = 40;
        this.setMaxUpStep(1.0F);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_PHASE, (byte) 0);
        this.entityData.define(DATA_TELEGRAPH, (byte) 0);
        this.entityData.define(DATA_TELEGRAPH_START, 0);
        this.entityData.define(DATA_TELEGRAPH_DURATION, 1);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ===================== 服务端主循环 =====================

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) this.level();
        if (!this.getPersistentData().getBoolean(SBSDValues.IS_INITIALIZED)) {
            // 与 Leader 相同的血量修饰增幅与 Leader 队伍。
            SBSDLeader.initializeLeader(this, this.getPersistentData());
        }
        if (this.invulnerableTicks > 0) {
            this.invulnerableTicks--;
            if (this.invulnerableTicks % 4 == 0) {
                WardenCombat.invulnerableShimmer(this);
            }
        }
        if (this.phaseChanged) {
            this.phaseChanged = false;
            this.onPhaseChanged();
        }
        this.tickTelegraph(level);
        this.tickBrain();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    private void tickBrain() {
        if (LeaderApi.isParried(this)) {
            if (this.currentAttack != null) {
                this.abortAttack();
            }
            this.getNavigation().stop();
            this.attackCooldown = Math.max(this.attackCooldown, this.attackInterval());
            return;
        }
        LivingEntity target = this.validTarget();
        if (this.currentAttack != null) {
            if (!this.currentAttack.step(this)) {
                WardenAttacks.Attack next = this.currentAttack.next(this);
                this.currentAttack.stop(this);
                this.currentAttack = next;
                if (next == null) {
                    this.attackCooldown = this.attackInterval();
                }
            }
            return;
        }
        if (target == null) {
            this.getNavigation().stop();
            return;
        }
        if (this.attackCooldown > 0 || this.invulnerableTicks > 0) {
            this.attackCooldown = Math.max(0, this.attackCooldown - 1);
            this.approach(target);
            return;
        }
        WardenAttacks.Move move = this.chooseMove(target);
        if (move == null) {
            this.approach(target);
            return;
        }
        this.getNavigation().stop();
        this.lastMove = move;
        this.currentAttack = move.create();
        ParryTutorial.announce(this, move);
    }

    @Nullable
    private WardenAttacks.Move chooseMove(LivingEntity target) {
        double distance = this.horizontalDistanceTo(target.position());
        List<WardenAttacks.Move> reds = new ArrayList<>();
        List<WardenAttacks.Move> blues = new ArrayList<>();
        for (WardenAttacks.Move move : WardenAttacks.Move.values()) {
            if (move.inRange(distance)) {
                (move.red ? reds : blues).add(move);
            }
        }
        if (this.bluesLeft <= 0 && this.redsLeft <= 0) {
            this.startRound();
        }
        // 缩地接的圆月、扇返不经过这里，整套只算一次蓝刀。
        boolean redDue = this.bluesLeft <= 0;
        List<WardenAttacks.Move> pool = redDue ? reds : blues;
        // 该出的颜色够不着时先出另一种，不计次数，该出的留到够得着时再出。
        boolean counted = !pool.isEmpty();
        if (!counted) {
            pool = redDue ? blues : reds;
        }
        if (pool.isEmpty()) {
            return null;
        }
        if (pool.size() > 1) {
            pool.remove(this.lastMove);
        }
        WardenAttacks.Move move = pool.get(this.random.nextInt(pool.size()));
        if (counted) {
            if (!redDue) {
                this.bluesLeft--;
            } else if (--this.redsLeft <= 0) {
                this.startRound();
            }
        }
        return move;
    }

    /** 按当前阶段重新决定这一轮的蓝刀数和红刀数。 */
    private void startRound() {
        int phase = this.getPhase();
        this.bluesLeft = this.rollCount(BLUE_RUN[phase]);
        this.redsLeft = this.rollCount(RED_RUN[phase]);
    }

    private int rollCount(int[] range) {
        return range[0] + this.random.nextInt(range[1] - range[0] + 1);
    }

    private void approach(LivingEntity target) {
        this.faceTarget();
        if (this.horizontalDistanceTo(target.position()) > APPROACH_DISTANCE) {
            if (--this.repathCooldown <= 0 || this.getNavigation().isDone()) {
                this.repathCooldown = 10;
                this.getNavigation().moveTo(target, 1.0D);
            }
        } else {
            this.getNavigation().stop();
        }
    }

    private int attackInterval() {
        return ATTACK_INTERVAL[this.getPhase()];
    }

    @Nullable
    private LivingEntity validTarget() {
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive()
                || target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            this.setTarget(null);
            return null;
        }
        if (target != null) {
            this.lastTargetPosition = target.position();
        }
        return target;
    }

    private void abortAttack() {
        if (this.currentAttack != null) {
            this.currentAttack.stop(this);
            this.currentAttack = null;
        }
        this.clearTelegraph();
        this.setNoGravity(false);
        if (LeaderApi.isParryable(this)) {
            LeaderApi.closeParryWindow(this);
        }
    }

    // ===================== 前摇（Leader 粒子环与招架窗口） =====================

    void telegraph(TelegraphKind kind, int duration) {
        long now = this.level().getGameTime();
        this.entityData.set(DATA_TELEGRAPH, (byte) kind.ordinal());
        this.entityData.set(DATA_TELEGRAPH_START, (int) now);
        this.entityData.set(DATA_TELEGRAPH_DURATION, Math.max(1, duration));
        this.telegraphEnd = now + duration;
        this.redWindowAt = now + duration - SBSDValues.PARRY_TICK;
        this.redWindowOpened = false;
    }

    void clearTelegraph() {
        this.entityData.set(DATA_TELEGRAPH, (byte) TelegraphKind.NONE.ordinal());
    }

    /**
     * 蓝刀：整段前摇显示 Leader 预警蓝火环（不可招架的信号）。
     * 红刀：不显示蓝火环，出招前 PARRY_TICK 打开招架窗口，之后由 Leader 核心在客户端绘制「危」、
     * 白→暗红粒子环并播放钟声（进度由 {@link #redWarningProgress()} 提供），与 MANAGED Leader 特殊攻击前一致。
     */
    private void tickTelegraph(ServerLevel level) {
        TelegraphKind kind = this.getTelegraphKind();
        if (kind == TelegraphKind.NONE) {
            return;
        }
        long now = level.getGameTime();
        if (kind == TelegraphKind.RED) {
            if (now >= this.redWindowAt && !this.redWindowOpened) {
                this.redWindowOpened = true;
                LeaderApi.openParryWindow(this, SBSDValues.PARRY_TICK + 2);
            }
            return;
        }
        SBSDLeader.doLeaderPreAttackIndicator(this, level, (int) (this.telegraphEnd - now));
    }

    public TelegraphKind getTelegraphKind() {
        return TelegraphKind.byId(this.entityData.get(DATA_TELEGRAPH));
    }

    /** 红刀招架窗口内的前摇进度（0 到 1），供 ClientLeaderIndicatorApi 提供器读取；没有红刀前摇时为空。 */
    public OptionalDouble redWarningProgress() {
        if (this.getTelegraphKind() != TelegraphKind.RED) {
            return OptionalDouble.empty();
        }
        long windowStart = (long) this.entityData.get(DATA_TELEGRAPH_START)
                + this.entityData.get(DATA_TELEGRAPH_DURATION) - SBSDValues.PARRY_TICK;
        double elapsed = (int) this.level().getGameTime() - windowStart;
        return OptionalDouble.of(elapsed / SBSDValues.PARRY_TICK);
    }

    // ===================== 阶段与锁血 =====================

    public int getPhase() {
        return Mth.clamp(this.entityData.get(DATA_PHASE), 0, BLUE_RUN.length - 1);
    }

    @Override
    public void setHealth(float health) {
        if (!this.allowLethal && !this.level().isClientSide) {
            int phase = this.getPhase();
            if (phase < LOCK_RATIOS.length) {
                float lock = this.getMaxHealth() * LOCK_RATIOS[phase];
                if (health < lock) {
                    if (this.getHealth() >= lock) {
                        health = lock;
                    }
                    this.entityData.set(DATA_PHASE, (byte) (phase + 1));
                    this.invulnerableTicks = LOCK_INVULNERABLE_TICKS;
                    this.phaseChanged = true;
                }
            }
        }
        super.setHealth(health);
    }

    private void onPhaseChanged() {
        this.abortAttack();
        this.getNavigation().stop();
        this.attackCooldown = Math.max(this.attackCooldown, LOCK_INVULNERABLE_TICKS);
        this.startRound();
        this.bossEvent.setColor(BAR_COLORS[this.getPhase()]);
        WardenCombat.phaseBurst(this);
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        // /kill、虚空等无视无敌的伤害不受锁血限制。
        this.allowLethal = source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        try {
            super.actuallyHurt(source, amount);
        } finally {
            this.allowLethal = false;
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.invulnerableTicks > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    // ===================== 移动与朝向（供招式使用） =====================

    Vec3 targetPosition() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.lastTargetPosition = target.position();
        }
        return this.lastTargetPosition != null ? this.lastTargetPosition : this.position();
    }

    double horizontalDistanceTo(Vec3 position) {
        double dx = position.x - this.getX();
        double dz = position.z - this.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    void faceTarget() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.faceTowards(target.position());
        }
    }

    void faceTowards(Vec3 position) {
        double dx = position.x - this.getX();
        double dz = position.z - this.getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            this.setFacing((float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F);
        }
    }

    void faceDirection(Vec3 direction) {
        if (direction.lengthSqr() > 1.0E-4D) {
            this.faceTowards(this.position().add(direction));
        }
    }

    void setFacing(float yaw) {
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.setXRot(0.0F);
    }

    /** 按位移移动（带碰撞），返回是否被水平方向挡住。 */
    boolean moveBy(Vec3 delta) {
        this.move(MoverType.SELF, delta);
        this.setDeltaMovement(Vec3.ZERO);
        return this.horizontalCollision;
    }

    /** 朝目标直线追击，返回是否已贴近到 stopDistance 以内。 */
    boolean chase(double speed, double stopDistance) {
        Vec3 aim = this.targetPosition();
        double distance = this.horizontalDistanceTo(aim);
        if (distance <= stopDistance) {
            return true;
        }
        Vec3 direction = new Vec3(aim.x - this.getX(), 0.0D, aim.z - this.getZ()).normalize();
        this.moveBy(direction.scale(Math.min(speed, distance - stopDistance)));
        return this.horizontalDistanceTo(aim) <= stopDistance;
    }

    List<LivingEntity> contacts(double inflate) {
        return this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(inflate),
                entity -> WardenCombat.canHit(this, entity));
    }

    boolean touchingTarget(double inflate) {
        LivingEntity target = this.getTarget();
        return target != null && this.getBoundingBox().inflate(inflate).intersects(target.getBoundingBox());
    }

    // ===================== 招架教程开关 =====================

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean disable = stack.is(Items.QUARTZ);
        if (disable || stack.is(Items.DIAMOND)) {
            if (player instanceof ServerPlayer serverPlayer) {
                ParryTutorial.setDisabled(serverPlayer, disable);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    // ===================== 首领血条 =====================

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    // ===================== 存档 =====================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("WardenPhase", (byte) this.getPhase());
        tag.putInt("WardenInvulnerable", this.invulnerableTicks);
        tag.putInt("WardenBluesLeft", this.bluesLeft);
        tag.putInt("WardenRedsLeft", this.redsLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        // 先恢复阶段，再由父类读取生命值，避免读档时按第一段锁血线误触发锁血。
        this.entityData.set(DATA_PHASE, tag.getByte("WardenPhase"));
        this.invulnerableTicks = tag.getInt("WardenInvulnerable");
        this.bluesLeft = tag.getInt("WardenBluesLeft");
        this.redsLeft = tag.getInt("WardenRedsLeft");
        super.readAdditionalSaveData(tag);
        this.bossEvent.setName(this.getDisplayName());
        this.bossEvent.setColor(BAR_COLORS[this.getPhase()]);
    }

    // ===================== 杂项 =====================

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEACON_DEACTIVATE;
    }
}
