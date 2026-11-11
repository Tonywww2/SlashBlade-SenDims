package com.tonywww.slashblade_sendims.compat.sjap;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDMobEffects;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 莉芙系列（sjap_adder）的整合包改动。
 * <ul>
 *     <li>「颂生者」：额外获得 10 秒增幅 III，并追加一道 2.0 倍斩击。</li>
 *     <li>「EX·段生铸念」：额外获得 12 秒增幅 V，并追加一道 4.0 倍斩击。</li>
 *     <li>「生露」：期间正常受伤，按攻击者记录受到的伤害；效果结束时，攻击者每造成
 *     玩家最大生命值 5% 的伤害，玩家对其造成一次 1.5 倍攻击力的魔法伤害，单个目标最多 8 次。</li>
 * </ul>
 * 这里只按注册名查找 sjap_adder 的效果，未安装该 mod 时这些监听器什么都不做。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class LifeSeries {

    public static final ResourceLocation LIFE_DEW_ID = new ResourceLocation("sjap_adder", "for_life");

    public static final int FOR_LIFE_AMPLIFY_TICKS = 200;
    public static final int FOR_LIFE_AMPLIFY_AMPLIFIER = 2;
    public static final double FOR_LIFE_SLASH_RATIO = 2.0d;

    public static final int FOR_LIFE_DRIVE_AMPLIFY_TICKS = 240;
    public static final int FOR_LIFE_DRIVE_AMPLIFY_AMPLIFIER = 4;
    public static final double FOR_LIFE_DRIVE_SLASH_RATIO = 4.0d;

    /** 每累计玩家最大生命值的这一比例，回敬一次 */
    public static final float DEW_STEP_RATIO = 0.05f;
    /** 每次回敬的伤害：玩家攻击力的倍数 */
    public static final float DEW_HIT_ATTACK_RATIO = 1.5f;
    /** 单次结算对同一目标的最多回敬次数 */
    public static final int DEW_MAX_HITS = 8;

    /** 生露期间的伤害记录：PersistentData 下的复合标签，键为攻击者 UUID，值为累计伤害 */
    public static final String DEW_RECORD_KEY = "sbsd_life_dew_record";

    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.35f, 1.0f, 0.45f), 1.2f);

    /** 结算回敬期间为 true：回敬伤害固定为 1.5 倍攻击力，不吃生露自身的增伤（见 ForLifesMixin）。 */
    private static boolean settling;

    private LifeSeries() {}

    // ------------------------------------------------------------------
    // 颂生者 / EX·段生铸念
    // ------------------------------------------------------------------

    public static void afterForLife(LivingEntity user, boolean drive) {
        if (user == null || user.level().isClientSide()) {
            return;
        }
        user.addEffect(new MobEffectInstance(SBSDMobEffects.AMPLIFY.get(),
                drive ? FOR_LIFE_DRIVE_AMPLIFY_TICKS : FOR_LIFE_AMPLIFY_TICKS,
                drive ? FOR_LIFE_DRIVE_AMPLIFY_AMPLIFIER : FOR_LIFE_AMPLIFY_AMPLIFIER));
        AttackManager.doSlash(user, 30.0f, Vec3.ZERO, false, true,
                drive ? FOR_LIFE_DRIVE_SLASH_RATIO : FOR_LIFE_SLASH_RATIO);
    }

    // ------------------------------------------------------------------
    // 生露
    // ------------------------------------------------------------------

    public static MobEffect lifeDew() {
        return ForgeRegistries.MOB_EFFECTS.getValue(LIFE_DEW_ID);
    }

    /** 在最终伤害阶段记录，记的是玩家实际承受的数值。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void recordDew(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Player) || victim.level().isClientSide() || event.getAmount() <= 0.0f) {
            return;
        }
        MobEffect dew = lifeDew();
        if (dew == null || !victim.hasEffect(dew)) {
            return;
        }
        LivingEntity attacker = resolveAttacker(event.getSource());
        if (attacker == null || attacker == victim) {
            return;
        }
        CompoundTag record = victim.getPersistentData().getCompound(DEW_RECORD_KEY);
        String key = attacker.getUUID().toString();
        record.putFloat(key, record.getFloat(key) + event.getAmount());
        victim.getPersistentData().put(DEW_RECORD_KEY, record);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDewExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && instance.getEffect() == lifeDew()) {
            settleDew(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDewRemoved(MobEffectEvent.Remove event) {
        MobEffect dew = lifeDew();
        if (dew != null && event.getEffect() == dew) {
            settleDew(event.getEntity());
        }
    }

    /** 结算并清空记录；返回本次结算造成的回敬次数（测试用）。 */
    public static int settleDew(LivingEntity holder) {
        if (holder == null || holder.level().isClientSide()) {
            return 0;
        }
        CompoundTag data = holder.getPersistentData();
        if (!data.contains(DEW_RECORD_KEY)) {
            return 0;
        }
        CompoundTag record = data.getCompound(DEW_RECORD_KEY);
        data.remove(DEW_RECORD_KEY);
        if (!holder.isAlive() || !(holder.level() instanceof ServerLevel level)) {
            return 0;
        }

        float step = holder.getMaxHealth() * DEW_STEP_RATIO;
        if (step <= 0.0f) {
            return 0;
        }
        float perHit = (float) (holder.getAttributeValue(Attributes.ATTACK_DAMAGE) * DEW_HIT_ATTACK_RATIO);
        if (perHit <= 0.0f) {
            return 0;
        }

        int total = 0;
        for (String key : record.getAllKeys()) {
            int hits = Math.min(DEW_MAX_HITS, (int) Math.floor(record.getFloat(key) / step));
            if (hits <= 0) {
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (!(level.getEntity(id) instanceof LivingEntity target) || !target.isAlive()) {
                continue;
            }
            DamageSource source = holder.damageSources().indirectMagic(holder, holder);
            settling = true;
            try {
                for (int i = 0; i < hits && target.isAlive(); i++) {
                    target.invulnerableTime = 0;
                    target.hurt(source, perHit);
                    total++;
                }
            } finally {
                settling = false;
            }
            target.invulnerableTime = 0;
            showDew(level, holder, target, hits);
        }
        return total;
    }

    public static boolean isSettling() {
        return settling;
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        Entity owner = source.getEntity();
        if (owner instanceof LivingEntity living) {
            return living;
        }
        Entity direct = source.getDirectEntity();
        return direct instanceof LivingEntity living ? living : null;
    }

    private static void showDew(ServerLevel level, LivingEntity holder, LivingEntity target, int hits) {
        Vec3 from = holder.position().add(0, holder.getBbHeight() * 0.6, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 delta = to.subtract(from);
        int points = Math.max(4, (int) (delta.length() * 3));
        for (int i = 0; i <= points; i++) {
            Vec3 p = from.add(delta.scale(i / (double) points));
            level.sendParticles(GREEN, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, to.x, to.y, to.z,
                6 + hits * 3, target.getBbWidth() * 0.5, target.getBbHeight() * 0.4, target.getBbWidth() * 0.5, 0.0);
        level.sendParticles(GREEN, to.x, to.y, to.z,
                10 + hits * 4, target.getBbWidth() * 0.6, target.getBbHeight() * 0.5, target.getBbWidth() * 0.6, 0.0);
        level.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.4f);
    }
}
