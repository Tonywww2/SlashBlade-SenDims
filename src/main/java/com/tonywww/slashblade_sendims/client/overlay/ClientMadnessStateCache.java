package com.tonywww.slashblade_sendims.client.overlay;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.se.FrenziedFlame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientMadnessStateCache {
    private static final Map<Integer, Integer> PENDING = new HashMap<>();

    private ClientMadnessStateCache() {
    }

    public static void accept(int entityId, int madness) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            PENDING.put(entityId, madness);
            return;
        }

        Entity entity = level.getEntity(entityId);
        if (entity instanceof LivingEntity livingEntity) {
            apply(livingEntity, madness);
        } else {
            PENDING.put(entityId, madness);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity livingEntity)) {
            return;
        }

        Integer madness = PENDING.remove(livingEntity.getId());
        if (madness != null) {
            apply(livingEntity, madness);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            PENDING.clear();
        }
    }

    private static void apply(LivingEntity entity, int madness) {
        entity.getPersistentData().putInt(FrenziedFlame.MADNESS_PATH, madness);
    }
}