package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.leader.SBSDLeader;
import com.tonywww.slashblade_sendims.SBSDValues;
import com.tonywww.slashblade_sendims.registeries.SBSDAttributes;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.event.SlashBladeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tracen.umapyoi.api.UmapyoiAPI;
import net.tracen.umapyoi.utils.UmaSoulUtils;
import twilightforest.entity.boss.AlphaYeti;
import twilightforest.entity.boss.KnightPhantom;
import twilightforest.entity.boss.Minoshroom;
import twilightforest.entity.boss.Naga;
import twilightforest.init.TFEntities;
import vazkii.botania.common.entity.BotaniaEntities;

import java.util.HashSet;

@Mod.EventBusSubscriber
public class LeaderEventListener {

    public static HashSet<EntityType<?>> DEFAULT_LEADER_SET =  new HashSet<>();

    static {
        DEFAULT_LEADER_SET.add(TFEntities.MINOSHROOM.get());
        DEFAULT_LEADER_SET.add(TFEntities.KNIGHT_PHANTOM.get());
        DEFAULT_LEADER_SET.add(TFEntities.ALPHA_YETI.get());
        DEFAULT_LEADER_SET.add(BotaniaEntities.DOPPLEGANGER);
    }

    @SubscribeEvent
    public static void LivingTickEventListener(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        CompoundTag persistentData = living.getPersistentData();
        if (persistentData.contains(SBSDValues.APOTH_BOSS)) {
            if (living.level() instanceof ServerLevel serverLevel) {
                SBSDLeader.tickLeader(living, serverLevel, persistentData, living.tickCount);

            }
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
            if (DEFAULT_LEADER_SET.contains(living.getType())) {
                persistentData.putBoolean(SBSDValues.BOSS_LEADER, true);
                persistentData.putBoolean(SBSDValues.APOTH_BOSS, true);
            }

            if (persistentData.contains(SBSDValues.APOTH_BOSS)) {
                SBSDLeader.initializeLeader(living, persistentData);

            } else if (event.getEntity().getType() == TFEntities.NAGA.get()) {
                persistentData.putBoolean(SBSDValues.BOSS_LEADER, true);

            } else if (false) {

            }

        }

    }

    @SubscribeEvent
    public static void HtiEventListener(SlashBladeEvent.HitEvent event) {
        LivingEntity target = event.getTarget();
        CompoundTag persistentData = target.getPersistentData();

        if (!(event.getUser() instanceof ServerPlayer serverPlayer)) return;
        ItemStack soul = UmapyoiAPI.getUmaSoul(serverPlayer);

        if (soul == null || soul.isEmpty()) return;

        if (persistentData.contains(SBSDValues.APOTH_BOSS)) {
            if (SBSDLeader.handleParryActions(event, target, persistentData)) {
                StunManager.setStun(target, 80);
            }
            gainAPbyHit(serverPlayer, soul, 1d);
            
        } else if (persistentData.contains(SBSDValues.BOSS_LEADER)) {
            if (target instanceof Naga naga) {
                if (SBSDLeader.handleParryActions(event, naga, persistentData)) {
                    naga.getMovementAI().doDaze();
                    naga.setCharging(false);
                    StunManager.setStun(target, 60);
                }
            }
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
