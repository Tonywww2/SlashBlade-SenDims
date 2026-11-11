package com.tonywww.slashblade_sendims.compat.biomespy;

import com.mojang.datafixers.util.Pair;
import com.tonywww.slashblade_sendims.SenDims;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.biome.Climate;

/**
 * Whether every surface climate point of a multi_noise biome source falls inside the ranges of
 * some entry. Vanilla picks the nearest entry, so when the entries leave gaps (SGJourney's Chulak
 * and Abydos, the Ad Astra planets, the pack's Nether, ...) biomes are used far outside their
 * declared ranges and a range-based pre-filter rejects places where they really generate.
 */
public final class ClimateCoverage {
    /** Interior points per axis in quantized climate units; the edges are skipped because noise can run past +-1. */
    private static final long[] GRID = {-9000L, -6000L, -3000L, 0L, 3000L, 6000L, 9000L};
    private static final Map<Climate.ParameterList<?>, Boolean> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private ClimateCoverage() {
    }

    public static boolean coversClimateSpace(Climate.ParameterList<?> parameters) {
        if (parameters == null) {
            return true;
        }
        Boolean cached = CACHE.get(parameters);
        if (cached != null) {
            return cached;
        }
        boolean covered = compute(parameters.values());
        CACHE.put(parameters, covered);
        if (!covered) {
            SenDims.LOGGER.info("BiomeSpy climate pre-filter disabled for a multi_noise biome source with {} entries: its ranges leave gaps, so biomes also generate outside them",
                    parameters.values().size());
        }
        return covered;
    }

    private static boolean compute(List<? extends Pair<Climate.ParameterPoint, ?>> values) {
        // min/max of temperature, humidity, continentalness, erosion and weirdness for entries that apply at the surface (depth 0).
        long[][] boxes = values.stream()
                .map(Pair::getFirst)
                .filter(point -> point.depth().min() <= 0L && point.depth().max() >= 0L)
                .map(point -> new long[] {
                        point.temperature().min(), point.temperature().max(),
                        point.humidity().min(), point.humidity().max(),
                        point.continentalness().min(), point.continentalness().max(),
                        point.erosion().min(), point.erosion().max(),
                        point.weirdness().min(), point.weirdness().max()})
                .toArray(long[][]::new);
        if (boxes.length == 0) {
            return false;
        }
        long[] climate = new long[5];
        int last = 0;
        for (long temperature : GRID) {
            climate[0] = temperature;
            for (long humidity : GRID) {
                climate[1] = humidity;
                for (long continentalness : GRID) {
                    climate[2] = continentalness;
                    for (long erosion : GRID) {
                        climate[3] = erosion;
                        for (long weirdness : GRID) {
                            climate[4] = weirdness;
                            last = findBox(boxes, climate, last);
                            if (last < 0) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    /** Starts from the previous hit: neighbouring grid points usually share an entry. */
    private static int findBox(long[][] boxes, long[] climate, int start) {
        for (int k = 0; k < boxes.length; k++) {
            int i = (start + k) % boxes.length;
            if (inside(boxes[i], climate)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean inside(long[] box, long[] climate) {
        for (int axis = 0; axis < climate.length; axis++) {
            if (climate[axis] < box[axis * 2] || climate[axis] > box[axis * 2 + 1]) {
                return false;
            }
        }
        return true;
    }
}
