package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber
public class FrenziedFlame extends SpecialEffect {

    public static final String MADNESS_PATH = "sbsd.sb.madness";

    public static final int BASE_MADNESS = 4;
    public static final int MADNESS_STUN_TICK = 50;

    public static final int MADNESS_BASE_DAMAGE_COUNT = 6;
    public static final int MADNESS_BADE_DAMAGE = 4;
    public static final int DAMAGE_CYCLE = 20;
    public static final int COUNT_CYCLE = 8;

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
        return Math.sqrt(level) / 5;
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

        addMadness(serverPlayer, serverPlayer, BASE_MADNESS);

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

        AttributeInstance instance = serverPlayer.getAttribute(Attributes.ATTACK_DAMAGE);
        if (instance == null) return;
        int finalMadness = (int) (BASE_MADNESS + (instance.getValue() * calcFernRatio(expLevel)));

        addMadness(event.getTarget(), serverPlayer, finalMadness);

    }

    public static void addMadness(LivingEntity target, LivingEntity attacker, int amount) {
        CompoundTag data = target.getPersistentData();
        if (!data.contains(MADNESS_PATH)) {
            data.putInt(MADNESS_PATH, amount);
        } else {
            int newMadness = data.getInt(MADNESS_PATH) + amount;

            if (newMadness >= target.getMaxHealth()) {
                doMadnessFullEffect(target, attacker, newMadness, MADNESS_BADE_DAMAGE, MADNESS_BASE_DAMAGE_COUNT);
                data.putInt(MADNESS_PATH, 0);
            } else {
                data.putInt(MADNESS_PATH, newMadness);

            }

            doMadnessParticle(target, (ServerLevel) target.level());

        }
    }

    public static void doMadnessParticle(LivingEntity target, ServerLevel serverLevel) {
        for (int i = 0; i < 3; i++) {
            double offsetX = (target.getRandom().nextDouble() - 0.5) * 0.5;
            double offsetY = target.getRandom().nextDouble() * target.getBbHeight();
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

    private static void doMadnessFullEffect(LivingEntity target, LivingEntity attacker, int madness, int baseDamage, int baseCount) {
        StunManager.setStun(target, MADNESS_STUN_TICK);
        target.addEffect(new MobEffectInstance(ALObjects.MobEffects.DETONATION.get(), MADNESS_STUN_TICK, 0));
        target.addEffect(new MobEffectInstance(ALObjects.MobEffects.GRIEVOUS.get(), MADNESS_STUN_TICK, 0));

        int damageCount = madness / DAMAGE_CYCLE;
        int damage =  baseDamage + damageCount;
        int countCount = (int) (Math.sqrt(madness) / COUNT_CYCLE);
        int maxDamageCount =  baseCount+ countCount;

        for (int i = 0; i < maxDamageCount; i += 2) {
            SenDims.serverScheduler.schedule(i, () -> {
                AttackManager.doAttackWith(attacker.damageSources().sonicBoom(attacker), damage, target, false, true);
                doMadnessParticle(target, (ServerLevel) target.level());

            });
        }

    }

}
