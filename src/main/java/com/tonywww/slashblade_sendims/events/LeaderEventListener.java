package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import com.tonywww.slashblade_sendims.SBSDValues;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;

@Mod.EventBusSubscriber
public class LeaderEventListener {

    @SubscribeEvent
    public static void LivingTickEventListener(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        CompoundTag persistentData = living.getPersistentData();
        if (!persistentData.contains(SBSDValues.APOTH_BOSS) || !persistentData.getBoolean(SBSDValues.APOTH_BOSS))
            return;

        if (living.level() instanceof ServerLevel serverLevel) {

            SBSDLeader.tickLeader(living, serverLevel, persistentData, living.tickCount);

        }

    }

    @SubscribeEvent
    public static void LivingHurtEventListener(LivingHurtEvent event) {
        LivingEntity living = event.getEntity();
        CompoundTag persistentData = living.getPersistentData();
        if (!persistentData.contains(SBSDValues.APOTH_BOSS) || !persistentData.getBoolean(SBSDValues.APOTH_BOSS))
            return;

        SBSDLeader.scaleIncomingDamage(event, persistentData);

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void EntityJoinLevelEventListener(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            CompoundTag persistentData = living.getPersistentData();
            if (!persistentData.contains(SBSDValues.APOTH_BOSS) || !persistentData.getBoolean(SBSDValues.APOTH_BOSS))
                return;

            SBSDLeader.initializeLeader(living, persistentData);
        }

    }

    @SubscribeEvent
    public static void HtiEventListener(SlashBladeEvent.HitEvent event) {
        LivingEntity target = event.getTarget();
        CompoundTag persistentData = target.getPersistentData();
        if (!persistentData.contains(SBSDValues.APOTH_BOSS) || !persistentData.getBoolean(SBSDValues.APOTH_BOSS))
            return;

        SBSDLeader.handleParryActions(event, target, persistentData);
        if (event.getUser() instanceof ServerPlayer serverPlayer) {
            ItemStack soul = UmapyoiAPI.getUmaSoul(serverPlayer);
            if (soul == null || soul.isEmpty()) return;

            UmaSoulUtils.addActionPoint(soul, SBSDValues.HIT_LEADER_AP);
        }

    }


}
