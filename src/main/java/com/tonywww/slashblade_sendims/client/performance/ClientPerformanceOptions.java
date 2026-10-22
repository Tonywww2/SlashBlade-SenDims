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
    public static volatile boolean minimapFilters = true;
    public static volatile boolean questBackground = true;
    public static volatile boolean questText = true;
    public static volatile boolean fixedBufferViews = true;
    public static volatile int grinderCacheBytes = 2 * 1024 * 1024;

    private ClientPerformanceOptions() {}

    @SubscribeEvent
    public static void configChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() != ClientPerformanceConfig.SPEC) return;
        grinderGeometry = ClientPerformanceConfig.GRINDER_GEOMETRY.get();
        slashbladeObj = ClientPerformanceConfig.SLASHBLADE_OBJ.get();
        minimapFilters = ClientPerformanceConfig.MINIMAP_FILTERS.get();
        questBackground = ClientPerformanceConfig.QUEST_BACKGROUND.get();
        questText = ClientPerformanceConfig.QUEST_TEXT.get();
        fixedBufferViews = ClientPerformanceConfig.FIXED_BUFFER_VIEWS.get();
        grinderCacheBytes = ClientPerformanceConfig.GRINDER_CACHE_MIB.get() * 1024 * 1024;
    }
}
