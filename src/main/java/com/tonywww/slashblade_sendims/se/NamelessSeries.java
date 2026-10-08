package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 无名王者系列（Nameless King）SE 公用常量与工具。
 * <p>
 * 该系列围绕「舍弃名字换取王权」的叙事，由褪名 → 王威 → 风暴 → 王权压制 逐层堆叠，
 * 终极刀「无冠」用 {@code 无冠} + {@code 王威·极} 替换掉最初的代价。
 */
public final class NamelessSeries {

    // ------------------------------------------------------------------
    // 共享 NBT 路径（均存放于玩家 PersistentData）
    // ------------------------------------------------------------------
    /** 「褪名」：上一次斩击命中的游戏刻 */
    public static final String FADED_LAST_HIT = "sbsd.se.nameless.faded_last_hit";
    /** 「王威」：本次结算周期内命中过的目标 UUID（成对 long 存储） */
    public static final String SOVEREIGNTY_TARGETS = "sbsd.se.nameless.targets";
    /** 「王威」：当前积攒的充能层数 */
    public static final String SOVEREIGNTY_CHARGE = "sbsd.se.nameless.charge";
    /** 真伤重入保护标志 */
    public static final String TRUE_DAMAGE_FLAG = "sbsd.se.nameless.true_damage";

    // ------------------------------------------------------------------
    // 数值配置
    // ------------------------------------------------------------------
    /** 「褪名」触发所需的空闲时长（tick） */
    public static final int FADED_IDLE_TICKS = 100;
    /** 「褪名」耐久上限钳制比例 */
    public static final float FADED_HIGH_RATIO = 0.70f;
    /** 「褪名」耐久下限钳制比例 */
    public static final float FADED_LOW_RATIO = 0.30f;

    /** 「王威」充能结算间隔（tick） */
    public static final int SOVEREIGNTY_INTERVAL = 5;
    /** 「王威」触发所需充能层数 */
    public static final int SOVEREIGNTY_THRESHOLD = 50;
    /** 「王威」触发时恢复的最大 AP 比例 */
    public static final double SOVEREIGNTY_AP_RATIO = 0.1d;
    /** 「王威·极」触发所需充能层数 */
    public static final int SOVEREIGNTY_EX_THRESHOLD = 40;
    /** 「王威·极」触发时恢复的最大 AP 比例 */
    public static final double SOVEREIGNTY_EX_AP_RATIO = 0.15d;

    /** 「风暴」赋予的药水效果时长（tick） */
    public static final int STORM_EFFECT_TICKS = 40;
    /** 「风暴」赋予的药水效果等级 */
    public static final int STORM_EFFECT_AMPLIFIER = 0;
    /** 「风暴」落雷伤害相对玩家攻击力的比例 */
    public static final float STORM_LIGHTNING_DAMAGE_RATIO = 0.4f;

    /** 「王权压制」对精英/Boss 附加的真实伤害比例 */
    public static final float SUPPRESSION_TRUE_DAMAGE_RATIO = 0.075f;

    /** 自定义真实伤害类型 */
    public static final ResourceKey<DamageType> TRUE_DAMAGE_TYPE =
            ResourceKey.create(Registries.DAMAGE_TYPE, SenDims.prefix("true_damage"));

    private NamelessSeries() {
    }

    /**
     * 取玩家主手拔刀剑的状态；非拔刀剑或缺少能力时返回 {@code null}。
     */
    public static ISlashBladeState getBladeState(LivingEntity entity) {
        ItemStack stack = entity.getMainHandItem();
        if (!(stack.getItem() instanceof ItemSlashBlade)) {
            return null;
        }
        return stack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
    }

    /**
     * 造成无视护甲、附魔、药水与抗性的真实伤害。
     * <p>
     * 带重入保护，避免 {@code LivingHurtEvent} 递归触发。
     */
    public static void dealTrueDamage(ServerPlayer player, LivingEntity target, float amount) {
        if (amount <= 0f || target == player || !target.isAlive()) {
            return;
        }

        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(TRUE_DAMAGE_FLAG)) {
            return;
        }

        data.putBoolean(TRUE_DAMAGE_FLAG, true);
        try {
            DamageSource source = new DamageSource(
                    player.level().registryAccess()
                            .registryOrThrow(Registries.DAMAGE_TYPE)
                            .getHolderOrThrow(TRUE_DAMAGE_TYPE),
                    player
            );
            target.invulnerableTime = 0;
            target.hurt(source, amount);
        } finally {
            data.putBoolean(TRUE_DAMAGE_FLAG, false);
        }
    }

    /**
     * 将刀的耐久钳制到指定比例（{@code ratio} 表示「剩余耐久」占比）。
     *
     * @return 是否实际发生了修改
     */
    public static boolean clampDurability(ServerPlayer player, ISlashBladeState state, float ratio) {
        int max = state.getMaxDamage();
        if (max <= 0) {
            return false;
        }

        int value = Math.round(max * (1.0f - ratio));
        value = Math.max(0, Math.min(max, value));
        if (state.getDamage() == value) {
            return false;
        }

        state.setDamage(value);

        // 与 BloodJade 相同：同步写入刀状态 NBT，保证客户端立刻刷新耐久条
        CompoundTag tag = player.getMainHandItem().getTag();
        if (tag != null && tag.contains("bladeState")) {
            tag.getCompound("bladeState").putInt("Damage", value);
        }
        return true;
    }

    /**
     * 记录/判定「褪名」的空闲状态。
     *
     * @return 本次命中前是否已满足「5 秒未命中」条件
     */
    public static boolean markFadedHit(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();
        long last = data.getLong(FADED_LAST_HIT);
        data.putLong(FADED_LAST_HIT, now);
        return now - last >= FADED_IDLE_TICKS;
    }

    /** 在目标位置生成视觉落雷与粒子（伤害由调用方处理） */
    public static void spawnLightningVisual(ServerLevel level, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                pos.x, pos.y + 1.0d, pos.z, 24, 0.35d, 1.0d, 0.35d, 0.35d);
        level.sendParticles(ParticleTypes.END_ROD,
                pos.x, pos.y + 1.0d, pos.z, 8, 0.25d, 1.0d, 0.25d, 0.05d);
    }

    /** 玩家 UUID → 成对 long 数组的追加（去重） */
    public static long[] appendUnique(long[] array, UUID id) {
        long msb = id.getMostSignificantBits();
        long lsb = id.getLeastSignificantBits();
        for (int i = 0; i + 1 < array.length; i += 2) {
            if (array[i] == msb && array[i + 1] == lsb) {
                return array;
            }
        }
        long[] next = java.util.Arrays.copyOf(array, array.length + 2);
        next[array.length] = msb;
        next[array.length + 1] = lsb;
        return next;
    }
}
