package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import mods.flammpfeil.slashblade.event.handler.EntitySpawnEventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class LeaderEventListener {

    @SubscribeEvent
    public static void LivingTickEventListener(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        CompoundTag persistentData = living.getPersistentData();
        if (!persistentData.contains(SBSDLeader.APOTH_BOSS) || !persistentData.getBoolean(SBSDLeader.APOTH_BOSS))
            return;

        if (living.level() instanceof ServerLevel serverLevel) {

            SBSDLeader.tickLeader(living, serverLevel, persistentData, living.tickCount);

        }

    }

    @SubscribeEvent
    public static void LivingHurtEventListener(LivingHurtEvent event) {
        LivingEntity living = event.getEntity();
        CompoundTag persistentData = living.getPersistentData();
        if (!persistentData.contains(SBSDLeader.APOTH_BOSS) || !persistentData.getBoolean(SBSDLeader.APOTH_BOSS))
            return;

        SBSDLeader.scaleIncomingDamage(event, persistentData);

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void EntityJoinLevelEventListener(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            CompoundTag persistentData = living.getPersistentData();
            if (!persistentData.contains(SBSDLeader.APOTH_BOSS) || !persistentData.getBoolean(SBSDLeader.APOTH_BOSS))
                return;

            SBSDLeader.initializeLeader(living, persistentData);
        }

    }


    @SubscribeEvent
    public static void HtiEventListener(SlashBladeEvent.HitEvent event) {
        LivingEntity target = event.getTarget();
        CompoundTag persistentData = target.getPersistentData();
        if (!persistentData.contains(SBSDLeader.APOTH_BOSS) || !persistentData.getBoolean(SBSDLeader.APOTH_BOSS))
            return;

        SBSDLeader.handleParryActions(event, target, persistentData);

    }


}
