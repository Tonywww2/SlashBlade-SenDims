package com.tonywww.slashblade_sendims.client.performance;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.config.ClientPerformanceConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/** Published once per config load/reload; hot vertex paths never query Forge's config maps. */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientPerformanceOptions {
    public static volatile boolean diagnostics;
    public static volatile boolean grinderGeometry = true;
    public static volatile boolean slashbladeObj = true;
    public static volatile boolean questBackground = true;
    public static volatile boolean questText = true;
    public static volatile boolean draconicEnergyCoreBounds = true;
    public static volatile boolean draconicTransparentPass = true;
    public static volatile boolean draconicModelGeometry = true;
    public static volatile boolean integratedDynamicsCableLight = true;
    public static volatile boolean integratedDynamicsCableOverlay = true;
    public static volatile boolean ysmPlayerShadows = false;
    public static volatile int grinderCacheBytes = 2 * 1024 * 1024;
    public static volatile int campfireSmokeDistance = 64;
    public static volatile int spawnerClientTickDistance = 64;
    public static volatile double xaeroWorldMapWriteRate = 1.0;

    private ClientPerformanceOptions() {}

    @SubscribeEvent
    public static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != ClientPerformanceConfig.SPEC) return;
        grinderGeometry = ClientPerformanceConfig.GRINDER_GEOMETRY.get();
        slashbladeObj = ClientPerformanceConfig.SLASHBLADE_OBJ.get();
        questBackground = ClientPerformanceConfig.QUEST_BACKGROUND.get();
        questText = ClientPerformanceConfig.QUEST_TEXT.get();
        draconicEnergyCoreBounds = ClientPerformanceConfig.DRACONIC_ENERGY_CORE_BOUNDS.get();
        draconicTransparentPass = ClientPerformanceConfig.DRACONIC_TRANSPARENT_PASS.get();
        draconicModelGeometry = ClientPerformanceConfig.DRACONIC_MODEL_GEOMETRY.get();
        integratedDynamicsCableLight = ClientPerformanceConfig.ID_CABLE_LIGHT.get();
        integratedDynamicsCableOverlay = ClientPerformanceConfig.ID_CABLE_OVERLAY.get();
        ysmPlayerShadows = ClientPerformanceConfig.YSM_PLAYER_SHADOWS.get();
        grinderCacheBytes = ClientPerformanceConfig.GRINDER_CACHE_MIB.get() * 1024 * 1024;
        campfireSmokeDistance = ClientPerformanceConfig.CAMPFIRE_SMOKE_DISTANCE.get();
        spawnerClientTickDistance = ClientPerformanceConfig.SPAWNER_CLIENT_TICK_DISTANCE.get();
        xaeroWorldMapWriteRate = ClientPerformanceConfig.XAERO_WORLD_MAP_WRITE_RATE.get();
    }
}
