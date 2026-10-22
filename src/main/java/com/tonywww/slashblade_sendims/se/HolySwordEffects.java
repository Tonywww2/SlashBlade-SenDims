package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.SBSDMobEffects;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.sa.HolySwordArts;
import mods.flammpfeil.slashblade.capability.concentrationrank.ConcentrationRankCapabilityProvider;
import mods.flammpfeil.slashblade.capability.concentrationrank.IConcentrationRank;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.item.SwordType;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/** Native SEs, per-blade hit charge, and the independent Mana Burst potion. */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID)
public final class HolySwordEffects {
    public static final String HIT_COUNT = "sbsd.holy_sword.normal_hits";
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.92F, 0.68F), 0.8F);

    private HolySwordEffects() {}

    public static boolean royal(ServerPlayer player, ISlashBladeState state) {
        return SEEventHandlers.isSEActive(state, player.experienceLevel, SBSDSpecialEffects.KINGS_SELECTION)
                && player.getCapability(ConcentrationRankCapabilityProvider.RANK_POINT)
                .map(rank -> rank.getRank(player.level().getGameTime()).level >= IConcentrationRank.ConcentrationRanks.A.level)
                .orElse(false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void normalHit(SlashBladeEvent.HitEvent event) {
        if (!(event.getUser() instanceof ServerPlayer player)) return;
        ISlashBladeState state = event.getSlashBladeState();
        if (state == null || !SEEventHandlers.isSEActive(state, player.experienceLevel, SBSDSpecialEffects.MANA_BURST)) return;
        // HitEvent is emitted by the successful native melee/item hook. Summoned
        // swords and damage-only effects do not pass through this hook.
        if (HolySwordArts.isCasting(player) || HolySwordArts.isCombo(state.getComboSeq())) return;
        ComboState combo = ComboStateRegistry.REGISTRY.get().getValue(state.resolvCurrentComboState(player));
        if (combo == null || combo.getPriority() <= 50) return;
        ItemStack blade = event.getBlade();
        int previous = blade.getOrCreateTag().getInt(HIT_COUNT);
        int hits = HolySwordRules.addHit(previous);
        blade.getOrCreateTag().putInt(HIT_COUNT, hits);
        boolean selected = royal(player, state);
        int required = HolySwordRules.requiredHits(selected);
        if (previous < required && hits >= required) {
            player.displayClientMessage(Component.translatable("message.slashblade_sendims.mana_burst_ready"), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.7F, 1.5F);
        } else if (hits < required) {
            player.displayClientMessage(Component.translatable("message.slashblade_sendims.mana_burst_charge", hits, required), true);
        }
    }

    /** Called before an accepted native SA combo's click action, after cancellation checks. */
    public static void acceptedArt(LivingEntity user, ISlashBladeState state) {
        if (state == null || !(user instanceof ServerPlayer player)
                || !SEEventHandlers.isSEActive(state, player.experienceLevel, SBSDSpecialEffects.MANA_BURST)) return;
        ItemStack blade = player.getMainHandItem();
        boolean selected = royal(player, state);
        if (!HolySwordRules.ready(blade.getOrCreateTag().getInt(HIT_COUNT), selected)) return;
        blade.getOrCreateTag().putInt(HIT_COUNT, 0);
        // Replace this effect rather than allowing vanilla's stronger/hidden effect
        // queue to resurrect an earlier duration. Each release grants exactly II/III.
        player.removeEffect(SBSDMobEffects.MANA_BURST.get());
        player.addEffect(new MobEffectInstance(SBSDMobEffects.MANA_BURST.get(),
                HolySwordRules.duration(selected), HolySwordRules.amplifier(selected), false, true, true));
        player.serverLevel().sendParticles(GOLD, player.getX(), player.getY() + 1.0D, player.getZ(),
                20, 0.45D, 0.65D, 0.45D, 0.02D);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void outgoingDamage(LivingHurtEvent event) {
        // Deliberately independent of the held item: the potion boosts all damage
        // credited to its bearer, including physical, magic and owned projectiles.
        if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        MobEffectInstance effect = attacker.getEffect(SBSDMobEffects.MANA_BURST.get());
        if (effect != null) event.setAmount((float) (event.getAmount()
                * HolySwordRules.damageMultiplier(effect.getAmplifier())));
    }

    public static boolean charging(LivingEntity user, ISlashBladeState state) {
        if (state == null || !user.isUsingItem() || user.getUsedItemHand() != InteractionHand.MAIN_HAND
                || user.getUseItem() != user.getMainHandItem() || state.isBroken() || state.isSealed()) return false;
        return user.getUseItem().getItem() instanceof ItemSlashBlade
                && SwordType.from(user.getUseItem()).contains(SwordType.ENCHANTED);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void incomingDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) return;
        ISlashBladeState state = NamelessSeries.getBladeState(player);
        if (!charging(player, state)
                || !SEEventHandlers.isSEActive(state, player.experienceLevel, SBSDSpecialEffects.INVISIBLE_AIR)) return;
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        // Use the attacker, not the arrow currently touching the player. Sources
        // without an attacker use their explicit origin (e.g. a positioned blast).
        Vec3 origin = attacker != null ? attacker.position() : source.getSourcePosition();
        if (origin != null && HolySwordRules.outsideWindRange(origin.distanceToSqr(player.position()))) {
            event.setAmount(event.getAmount() * HolySwordRules.WIND_MULTIPLIER);
        }
    }

}
