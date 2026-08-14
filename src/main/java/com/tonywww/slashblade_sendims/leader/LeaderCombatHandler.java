package com.tonywww.slashblade_sendims.leader;

import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.ParryResult;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.util.AttackManager;
import mods.flammpfeil.slashblade.util.KnockBacks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public final class LeaderCombatHandler {
    private static final int DEFAULT_STUN_TICKS = 80;

    private LeaderCombatHandler() {
    }

    public static boolean handleSlashBladeParry(SlashBladeEvent.HitEvent event, LivingEntity target) {
        ResourceLocation currentCombo = event.getSlashBladeState().getComboSeq();
        if (!SBSDValues.PARRY_COMBOS.contains(currentCombo)) {
            return false;
        }
        ParryResult result = LeaderManager.tryParry(target, event.getUser(), currentCombo);
        if (!result.isAccepted()) {
            return false;
        }

        LivingEntity user = event.getUser();
        AttackManager.doSlash(user, 45.0F, 0x6cf243, Vec3.ZERO,
                false, false, 1.25f, KnockBacks.smash);
        float amount = (float) SBSDAttributes.getAttributeValue(user, SBSDAttributes.PARRY_HEAL_AMOUNT.get());
        user.heal(amount + user.getMaxHealth() * 0.2f);
        return true;
    }

    public static void applyParriedReaction(LivingEntity target) {
        applyParriedReaction(target, DEFAULT_STUN_TICKS);
    }

    public static void applyParriedReaction(LivingEntity target, int stunTicks) {
        StunManager.setStun(target, stunTicks);
    }

    static int defaultStunTicks() {
        return DEFAULT_STUN_TICKS;
    }

    public static void scaleIncomingDamage(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.getPersistentData().contains(SBSDValues.APOTH_BOSS)
                && !target.getPersistentData().getBoolean(SBSDValues.APOTH_BOSS)
                && !LeaderStateStorage.hasExplicitProfile(target)) {
            return;
        }
        if (LeaderApi.isParried(target)) {
            event.setAmount(event.getAmount() * SBSDValues.PARRIED_DAMAGE_SCALE);
        }
    }
}