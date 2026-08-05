package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.leader.ExternalLeaderController;
import com.tonywww.slashblade_sendims.leader.LeaderCombatHandler;
import com.tonywww.slashblade_sendims.leader.LeaderManager;
import com.tonywww.slashblade_sendims.leader.ManagedLeaderController;
import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import twilightforest.entity.boss.Naga;
import twilightforest.init.TFEntities;

@Mod.EventBusSubscriber
public class LeaderEventListener {

    @SubscribeEvent
    public static void LivingTickEventListener(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (!(living.level() instanceof ServerLevel serverLevel)
                || living instanceof Naga
                || !LeaderApi.isLeader(living)) {
            return;
        }
        LeaderProfile profile = LeaderApi.getSnapshot(living).orElseThrow().profile();
        if (profile == LeaderProfile.MANAGED) {
            ManagedLeaderController.tick(living, serverLevel);
        } else {
            ExternalLeaderController.tick(living, serverLevel);
        }
    }

    @SubscribeEvent
    public static void LivingHurtEventListener(LivingHurtEvent event) {
        LivingEntity living = event.getEntity();
        if (!LeaderApi.isLeader(living))
            return;
        LeaderCombatHandler.scaleIncomingDamage(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void EntityJoinLevelEventListener(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)
                || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (SBSDValues.DEFAULT_LEADER_SET.contains(living.getType())) {
            living.getPersistentData().putBoolean(SBSDValues.BOSS_LEADER, true);
            living.getPersistentData().putBoolean(SBSDValues.APOTH_BOSS, true);
        }
        if (living.getType() == TFEntities.NAGA.get()) {
            LeaderManager.registerLeader(living, LeaderProfile.EXTERNAL);
        } else {
            LeaderManager.applyRegistration(living);
        }
    }

    @SubscribeEvent
    public static void StartTrackingEventListener(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer
                && event.getTarget() instanceof LivingEntity living) {
            LeaderManager.syncTo(serverPlayer, living);
        }
    }

    @SubscribeEvent
    public static void HtiEventListener(SlashBladeEvent.HitEvent event) {
        LivingEntity target = event.getTarget();
        if (!(event.getUser() instanceof ServerPlayer serverPlayer)) return;
        ItemStack soul = UmapyoiAPI.getUmaSoul(serverPlayer);

        if (soul == null || soul.isEmpty()) return;

        if (LeaderApi.isLeader(target)) {
            LeaderCombatHandler.handleSlashBladeParry(event, target);
            gainAPbyHit(serverPlayer, soul, 1d);
        } else {
            gainAPbyHit(serverPlayer, soul, 0.25d);
        }

    }

    private static void gainAPbyHit(ServerPlayer serverPlayer, ItemStack soul, double ratio) {
        double gain = SBSDValues.HIT_LEADER_AP * ratio;
        AttributeInstance attributeInstance = serverPlayer.getAttribute(SBSDAttributes.AP_GAIN_PERCENTAGE.get());
        if (attributeInstance != null) gain *= attributeInstance.getValue();
        UmaSoulUtils.addActionPoint(soul, (int) gain);

    }

}
