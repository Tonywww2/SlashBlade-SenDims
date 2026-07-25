package com.tonywww.slashblade_sendims.compat.draconicevolution;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DraconicEvolutionCompat {
    private static final Set<SimplexNoise> TARGET_END_ISLAND_NOISES = ConcurrentHashMap.newKeySet();
    private static final Set<TheEndBiomeSource> TARGET_END_BIOME_SOURCES = ConcurrentHashMap.newKeySet();

    private DraconicEvolutionCompat() {
    }

    public static boolean isTargetDimension(WorldGenLevel level) {
        return level.getLevel().dimension().location().equals(DraconicEvolutionCompatConfig.targetDimension());
    }

    public static boolean isTargetEndIslandNoise(SimplexNoise noise) {
        return TARGET_END_ISLAND_NOISES.contains(noise);
    }

    public static boolean isTargetEndBiomeSource(TheEndBiomeSource biomeSource) {
        return TARGET_END_BIOME_SOURCES.contains(biomeSource);
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && isTargetDimension(level)) {
            trackLevel(level, true);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && isTargetDimension(level)) {
            trackLevel(level, false);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TARGET_END_ISLAND_NOISES.clear();
        TARGET_END_BIOME_SOURCES.clear();
    }

    private static void trackLevel(ServerLevel level, boolean add) {
        BiomeSource biomeSource = level.getChunkSource().getGenerator().getBiomeSource();
        if (biomeSource instanceof TheEndBiomeSource endBiomeSource) {
            update(TARGET_END_BIOME_SOURCES, endBiomeSource, add);
        }

        level.getChunkSource().randomState().router().mapAll(function -> {
            if (function instanceof EndIslandNoiseAccessor accessor) {
                update(TARGET_END_ISLAND_NOISES, accessor.slashblade_sendims$getIslandNoise(), add);
            }
            return function;
        });
    }

    private static <T> void update(Set<T> instances, T instance, boolean add) {
        if (add) {
            instances.add(instance);
        } else {
            instances.remove(instance);
        }
    }
}