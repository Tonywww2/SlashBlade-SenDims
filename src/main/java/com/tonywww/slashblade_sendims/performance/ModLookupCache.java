package com.tonywww.slashblade_sendims.performance;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModLookupCache {
    private static volatile LoadedModSnapshot snapshot;

    private ModLookupCache() {}

    public static LoadedModSnapshot snapshot() {
        return snapshot;
    }

    @SubscribeEvent
    public static void loadingComplete(FMLLoadCompleteEvent event) {
        Map<String, String> versions = new HashMap<>();
        ModList.get().getMods().forEach(mod -> versions.put(mod.getModId(), mod.getVersion().toString()));
        // Publish only a completed snapshot. Calls made during loading retain the upstream path.
        snapshot = new LoadedModSnapshot(versions);
    }
}
