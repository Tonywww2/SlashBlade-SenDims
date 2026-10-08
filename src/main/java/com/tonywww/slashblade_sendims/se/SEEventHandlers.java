package com.tonywww.slashblade_sendims.se;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.sa.NamelessThreefold;
import com.tonywww.slashblade_sendims.registeries.SBSDSpecialEffects;
import com.tonywww.slashblade_sendims.utils.TetraUtils;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
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

        long gameTime = livingEntity.level().getGameTime();

        if (gameTime % 10 == 5) {
            // FrenziedFlame logic
            FrenziedFlame.onLivingTick(livingEntity);
        }

        // 无名王者系列 · 王威充能结算（每 5 tick）
        if (gameTime % NamelessSeries.SOVEREIGNTY_INTERVAL == 0) {
            onNamelessSovereigntyTick(livingEntity);
        }
    }

    /**
     * 无名王者系列「王威 / 王威·极」的充能结算。
     * 两者共用同一套逻辑，仅阈值与 AP 恢复比例不同。
     */
    private static void onNamelessSovereigntyTick(LivingEntity livingEntity) {
        if (!(livingEntity instanceof ServerPlayer player)) return;

        ISlashBladeState state = NamelessSeries.getBladeState(player);
        if (state == null) return;

        int expLevel = player.experienceLevel;

        if (isSEActive(state, expLevel, SBSDSpecialEffects.NAMELESS_SE_SOVEREIGNTY)) {
            NamelessSovereignty.onPlayerTick(player,
                    NamelessSeries.SOVEREIGNTY_THRESHOLD, NamelessSeries.SOVEREIGNTY_AP_RATIO);
        } else if (isSEActive(state, expLevel, SBSDSpecialEffects.NAMELESS_SE_SOVEREIGNTY_EX)) {
            NamelessSovereignty.onPlayerTick(player,
                    NamelessSeries.SOVEREIGNTY_EX_THRESHOLD, NamelessSeries.SOVEREIGNTY_EX_AP_RATIO);
        }
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

        // BlessingAndBane logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.BLESSING_AND_BANE)) {
            BlessingAndBane.onDoSlash(serverPlayer);
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
            FrenziedFlame.onHit(serverPlayer, event.getTarget(), state, expLevel);
        }

        // Aftershock logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.AFTERSHOCK)) {
            Aftershock.onHit(serverPlayer, event.getTarget());
        }

        // ArmorMelt logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.ARMOR_MELT)) {
            ArmorMelt.onHit(serverPlayer, event.getTarget());
        }

        // Mahakala logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.MAHAKALA)) {
            Mahakala.onHit(serverPlayer, event.getTarget());
        }

        // ---- 无名王者系列 ----
        // 褪名：空闲 5 秒后的命中会钳制耐久
        if (isSEActive(state, expLevel, SBSDSpecialEffects.NAMELESS_SE_FADED)) {
            NamelessFaded.onHit(serverPlayer, state);
        }

        // 王威 / 王威·极：登记王之名录
        if (isSEActive(state, expLevel, SBSDSpecialEffects.NAMELESS_SE_SOVEREIGNTY)
                || isSEActive(state, expLevel, SBSDSpecialEffects.NAMELESS_SE_SOVEREIGNTY_EX)) {
            NamelessSovereignty.onHit(serverPlayer, event.getTarget());
        }
    }

    /**
     * 无名王者系列「风暴」：SA 释放（剑技成立）时结算。
     * <p>
     * 注意：SlashBlade 的 {@code ChargeActionEvent} 是「蓄力中每 tick」的事件，
     * 真正表示 SA 成立的是 {@code PerformSlashArtEvent}。
     */
    @SubscribeEvent
    public static void onPerformSlashArt(SlashBladeEvent.PerformSlashArtEvent event) {
        // This SA owns its once-per-cast storm trigger, after the pack's AP cancellation.
        if (NamelessThreefold.isCombo(event.getComboState())) return;
        LivingEntity livingEntity = event.getEntityLiving();
        if (livingEntity.level().isClientSide()) return;
        if (!(livingEntity instanceof ServerPlayer serverPlayer)) return;

        ISlashBladeState state = event.getSlashBladeState();
        if (state == null) return;
        if (event.getType() == SlashArts.ArtsType.Fail) return;

        int expLevel = serverPlayer.experienceLevel;

        if (isSEActive(state, expLevel, SBSDSpecialEffects.NAMELESS_SE_STORM)) {
            NamelessStorm.onChargeAction(serverPlayer, state);
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

        // Mahakala logic
        if (isSEActive(state, expLevel, SBSDSpecialEffects.MAHAKALA)) {
            Mahakala.onSlashBladeUpdate(player, event);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        float originalDamage = event.getAmount();
        Entity attackerEntity = source.getEntity();

        // Mahakala logic
        Mahakala.onLivingHurt(event, attackerEntity, target, originalDamage);

        if (!(attackerEntity instanceof ServerPlayer player)) {
            return;
        }

        ItemStack bladeStack = player.getMainHandItem();
        if (!(bladeStack.getItem() instanceof ItemSlashBlade)) return;
        ISlashBladeState state = bladeStack.getCapability(ItemSlashBlade.BLADESTATE).orElse(null);
        if (state == null) return;

        int a = TetraUtils.getEffectLvlTotal(player, SBSDValues.MANA_RESONANCE);
        boolean isMagic = source.is(DamageTypeTags.WITCH_RESISTANT_TO);
        int experienceLevel = player.experienceLevel;

        // ManaDetonation logic
        if (isSEActive(state, experienceLevel, SBSDSpecialEffects.MANA_DETONATION)) {
            ManaDetonation.onLivingHurt(event, bladeStack, isMagic, a);
        }

        // InvinciblePierce logic
        if (isSEActive(state, experienceLevel, SBSDSpecialEffects.INVINCIBLE_PIERCE)) {
            if (!source.is(DamageTypes.SONIC_BOOM)) {
                InvinciblePierce.onLivingHurt(player, target, originalDamage, isMagic, a);
            }
        }

        // DistantThunder logic
        if (isSEActive(state, experienceLevel, SBSDSpecialEffects.DISTANT_THUNDER)) {
            DistantThunder.onLivingHurt(player, target, originalDamage, isMagic);
        }

        // ---- 无名王者系列 ----
        // 王权压制：仅在本体近战/斩击命中（玩家攻击、直接来源为玩家）时结算，
        // 避免与落雷、真实伤害等附加效果互相触发。
        if (isSEActive(state, experienceLevel, SBSDSpecialEffects.NAMELESS_SE_SUPPRESSION)
                && source.is(DamageTypes.PLAYER_ATTACK)
                && source.getDirectEntity() == player) {
            NamelessSuppression.onLivingHurt(player, target, originalDamage);
        }
    }
}
