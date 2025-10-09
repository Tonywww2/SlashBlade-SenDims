package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.MadnessSyncPacket;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import dev.shadowsoffire.attributeslib.api.ALObjects;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

@Mod.EventBusSubscriber
public class FrenziedFlame extends SpecialEffect {

    public static final String MADNESS_PATH = "sbsd.sb.madness";
    public static final String MADNESS_REGIN_CD_PATH = "sbsd.sb.madness_regin";

    public static final int MADNESS_REGIN_CD = 5;

    public static final int BASE_MADNESS = 5;
    public static final int MADNESS_STUN_TICK = 50;

    public static final int MADNESS_BASE_DAMAGE_COUNT = 3;
    public static final int MADNESS_BADE_DAMAGE = 3;

    public static final DustColorTransitionOptions FRENZY_PARTICLE_1 = new DustColorTransitionOptions(
            new Vector3f(1.0f, 0.6f, 0.0f),
            new Vector3f(1.0f, 0.8f, 0.2f),
            1.0f
    );

    public static final DustColorTransitionOptions FRENZY_PARTICLE_2 = new DustColorTransitionOptions(
            new Vector3f(0.9f, 0.4f, 0.0f),
            new Vector3f(1.0f, 0.9f, 0.3f),
            1.2f
    );

    public FrenziedFlame() {
        super(30);
    }

    public static double calcFernRatio(int level) {
        return Math.min(5d, Math.sqrt(level) / 5d);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.level().isClientSide()) return;
        if (livingEntity.level().getGameTime() % 10 != 5) return;

        CompoundTag data = livingEntity.getPersistentData();
        if (data.contains(MADNESS_PATH)) {
            int currentMadness = data.getInt(MADNESS_PATH);
            if (currentMadness > 0) {
                if (!data.contains(MADNESS_REGIN_CD_PATH) || data.getInt(MADNESS_REGIN_CD_PATH) <= 0) {
                    int madnessRegen = (int) (livingEntity.getMaxHealth() * 0.1f) + 5;
                    addMadness(livingEntity, livingEntity, -madnessRegen);

                } else {
                    data.putInt(MADNESS_REGIN_CD_PATH, data.getInt(MADNESS_REGIN_CD_PATH) - 1);

                }

            }

        }

    }

    @SubscribeEvent
    public static void onDoSlash(SlashBladeEvent.DoSlashEvent event) {
        LivingEntity livingEntity = event.getUser();
        if (livingEntity.level().isClientSide()) return;
        if (!(livingEntity instanceof ServerPlayer serverPlayer)) return;

        int expLevel = serverPlayer.experienceLevel;
        if (!SpecialEffect.isEffective(SBSDSpecialEffects.FRENZIED_FLAME.get(), expLevel)) return;
        ItemStack bladeStack = serverPlayer.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = event.getSlashBladeState();
        if (!state.hasSpecialEffect(SBSDSpecialEffects.FRENZIED_FLAME.getId())) return;

        addMadness(serverPlayer, serverPlayer, (int) (BASE_MADNESS + (serverPlayer.getHealth() * 0.015d)));

    }

    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity livingEntity = event.getUser();
        if (livingEntity.level().isClientSide()) return;
        if (!(livingEntity instanceof ServerPlayer serverPlayer)) return;

        int expLevel = serverPlayer.experienceLevel;
        if (!SpecialEffect.isEffective(SBSDSpecialEffects.FRENZIED_FLAME.get(), expLevel)) return;
        ItemStack bladeStack = serverPlayer.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = event.getSlashBladeState();
        if (!state.hasSpecialEffect(SBSDSpecialEffects.FRENZIED_FLAME.getId())) return;

        int finalMadness = getFinalMadness(serverPlayer, expLevel);
        addMadness(event.getTarget(), serverPlayer, finalMadness);

    }

    public static int getFinalMadness(LivingEntity attacker, int expLevel) {
        AttributeInstance instance = attacker.getAttribute(Attributes.ATTACK_DAMAGE);
        return instance == null ?
                BASE_MADNESS :
                (int) (BASE_MADNESS + (instance.getValue() * calcFernRatio(expLevel)));

    }

    public static void addMadness(LivingEntity target, LivingEntity attacker, int amount) {
        CompoundTag data = target.getPersistentData();
        if (amount > 0) {
            AttributeInstance instance = target.getAttribute(SBSDAttributes.MADNESS_REDUCE.get());
            amount = instance == null ?
                    amount :
                    (int) (amount - instance.getValue());
            if (!data.contains(MADNESS_PATH)) {
                data.putInt(MADNESS_PATH, amount);
            } else {
                int newMadness = data.getInt(MADNESS_PATH) + amount;

                if (newMadness >= target.getMaxHealth()) {
                    doFrenzyFullEffect(target, attacker, newMadness);
                    data.putInt(MADNESS_PATH, 0);
                } else {
                    data.putInt(MADNESS_PATH, Math.max(0, newMadness));
                    doMadnessParticle(target, (ServerLevel) target.level());

                }
                data.putInt(MADNESS_REGIN_CD_PATH, MADNESS_REGIN_CD);

            }
        } else {
            int newMadness = data.getInt(MADNESS_PATH) + amount;
            data.putInt(MADNESS_PATH, Math.max(0, newMadness));
        }

        if (target instanceof ServerPlayer serverPlayer) {
            SenDims.NETWORK.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new MadnessSyncPacket(target.getId(), data.getInt(MADNESS_PATH)));
        }
    }

    public static void doMadnessParticle(LivingEntity target, ServerLevel serverLevel) {
        for (int i = 0; i < 3; i++) {
            double offsetX = (target.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetY = target.getBbHeight() + target.getRandom().nextDouble() * 0.5;
            double offsetZ = (target.getRandom().nextDouble() - 0.5) * 0.5;

            serverLevel.sendParticles(
                    FRENZY_PARTICLE_1,
                    target.getX() + offsetX,
                    target.getY() + offsetY,
                    target.getZ() + offsetZ,
                    1, 0.1d, 0.05d, 0.1d, 0
            );

            serverLevel.sendParticles(
                    FRENZY_PARTICLE_2,
                    target.getX() + offsetX,
                    target.getY() + offsetY,
                    target.getZ() + offsetZ,
                    3, 0.15d, 0.1d, 0.15d, 0
            );
        }
    }

    private static void doFrenzyFullEffect(LivingEntity target, LivingEntity attacker, int madness) {
        StunManager.setStun(target, MADNESS_STUN_TICK);
        target.addEffect(new MobEffectInstance(ALObjects.MobEffects.DETONATION.get(), MADNESS_STUN_TICK, 0));
        target.addEffect(new MobEffectInstance(ALObjects.MobEffects.GRIEVOUS.get(), MADNESS_STUN_TICK, 0));

        AttributeInstance instanceFrenzyDamage = attacker.getAttribute(SBSDAttributes.FRENZY_DAMAGE.get());
        float totalDamage = instanceFrenzyDamage == null ?
                madness * 0.5f :
                (float) (madness * 0.5f * (1d + instanceFrenzyDamage.getValue()));
        int originCount = (int) (Math.log(madness) * 2 + 1);

        AttributeInstance instanceFrenzyResistance = target.getAttribute(SBSDAttributes.FRENZY_RESISTANCE.get());
        double damageRatio = instanceFrenzyResistance == null ?
                1d :
                1d - instanceFrenzyResistance.getValue();

        float finalDamage = (float) (((totalDamage / originCount) + FrenziedFlame.MADNESS_BADE_DAMAGE) * damageRatio);
        int finalCount = originCount + FrenziedFlame.MADNESS_BASE_DAMAGE_COUNT;

        if (finalDamage > 0) {
            for (int i = 0; i < finalCount; i += 2) {
                SenDims.serverScheduler.schedule(i, () -> {
                    AttackManager.doAttackWith(attacker.damageSources().sonicBoom(attacker), finalDamage, target, false, true);
                    doMadnessParticle(target, (ServerLevel) target.level());

                });
            }

        }

    }

}
