package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.utils.TetraUtils;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber
public class SEEventHandlers {

    public static boolean isSEActive(ISlashBladeState state, int expLevel, RegistryObject<SpecialEffect> seReg) {
        return SpecialEffect.isEffective(seReg.get(), expLevel) && state.hasSpecialEffect(seReg.getId());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.level().isClientSide()) return;
        if (livingEntity.level().getGameTime() % 10 != 5) return;

        // FrenziedFlame logic
        FrenziedFlame.onLivingTick(livingEntity);
    }

    @SubscribeEvent
    public static void onDoSlash(SlashBladeEvent.DoSlashEvent event) {
        LivingEntity livingEntity = event.getUser();
        if (livingEntity.level().isClientSide()) return;
        if (!(livingEntity instanceof ServerPlayer serverPlayer)) return;

        ISlashBladeState state = event.getSlashBladeState();
        if (state == null) return;
        int expLevel = serverPlayer.experienceLevel;

        // FrenziedFlame logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.FRENZIED_FLAME)) {
            FrenziedFlame.onDoSlash(serverPlayer, state, expLevel);
        }
    }

    @SubscribeEvent
    public static void onHit(SlashBladeEvent.HitEvent event) {
        LivingEntity livingEntity = event.getUser();
        if (livingEntity.level().isClientSide()) return;
        if (!(livingEntity instanceof ServerPlayer serverPlayer)) return;

        ISlashBladeState state = event.getSlashBladeState();
        if (state == null) return;
        int expLevel = serverPlayer.experienceLevel;

        // FrenziedFlame logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.FRENZIED_FLAME)) {
            boolean withArcane = isSEActive(state, expLevel, SBSDSpecialEffects.ARCANE_A);
            boolean withThreeFingers = isSEActive(state, expLevel, SBSDSpecialEffects.THREE_FINGERS);
            int finalMadness = FrenziedFlame.getFinalMadness(serverPlayer, expLevel, withArcane, withThreeFingers);
            FrenziedFlame.addMadness(event.getTarget(), serverPlayer, finalMadness);
        }

        // Aftershock logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.AFTERSHOCK)) {
            Aftershock.onHit(serverPlayer, event.getTarget());
        }
    }

    @SubscribeEvent
    public static void onSlashBladeUpdate(SlashBladeEvent.UpdateEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ISlashBladeState state = event.getSlashBladeState();
        if (state == null) return;
        if (!event.isSelected()) return;

        int expLevel = player.experienceLevel;

        // ManaDetonation logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.MANA_DETONATION)) {
            ManaDetonation.onSlashBladeUpdate(player, event);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float originalDamage = event.getAmount();
        Entity attackerEntity = source.getEntity();

        if (!(attackerEntity instanceof ServerPlayer player)) {
            return;
        }

        ItemStack bladeStack = player.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = bladeStack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
        if (state == null) return;

        int a = TetraUtils.getEffectLvlTotal(player, SBSDValues.MANA_RESONANCE);
        boolean isMagic = source.is(DamageTypeTags.WITCH_RESISTANT_TO);

        // ManaDetonation logic
        if (isSEActive(state, player.experienceLevel, SBSDSpecialEffects.MANA_DETONATION)) {
            ManaDetonation.onLivingHurt(event, bladeStack, isMagic, a);
        }

        // InvinciblePierce logic
        if (isSEActive(state, player.experienceLevel, SBSDSpecialEffects.INVINCIBLE_PIERCE)) {
            if (!source.is(DamageTypes.SONIC_BOOM)) {
                InvinciblePierce.onLivingHurt(player, target, originalDamage, isMagic, a);
            }
        }

        // DistantThunder logic
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            int expLevel = player.experienceLevel;
            if (isSEActive(state, expLevel, SBSDSpecialEffects.DISTANT_THUNDER)) {
                DistantThunder.onLivingHurt(player, target, originalDamage);
            }
        }
    }
}
