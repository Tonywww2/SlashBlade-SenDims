package com.tonywww.slashblade_sendims.worldgen;

import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class SaturnRingAsteroidField {
    private static final int CELL_SIZE = 32;
    private static final int VERTICAL_CELL_SIZE = 32;

    private SaturnRingAsteroidField() {
    }

    public static boolean isInsideAsteroid(int x, int y, int z, SimplexNoise shapeNoise) {
        int cellX = Math.floorDiv(x, CELL_SIZE);
        int cellY = Math.floorDiv(y, VERTICAL_CELL_SIZE);
        int cellZ = Math.floorDiv(z, CELL_SIZE);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Asteroid asteroid = asteroidForCell(cellX + dx, cellY + dy, cellZ + dz);
                    if (asteroid != null && asteroid.contains(x, y, z, shapeNoise)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isInsideTransitionBridge(int x, int y, int z, int ringYCenter, SimplexNoise shapeNoise) {
        int verticalDistance = Math.abs(y - ringYCenter);
        if (verticalDistance > 12) {
            return false;
        }

        double falloff = 1.0 - (verticalDistance / 12.0);
        double broadNoise = shapeNoise.getValue(x * 0.040 + 1200.0, z * 0.040 - 1200.0);
        double detailNoise = shapeNoise.getValue(x * 0.120 - 400.0, y * 0.060, z * 0.120 + 400.0) * 0.35;
        double threshold = 0.18 - falloff * 0.55;
        return broadNoise + detailNoise > threshold;
    }

    private static Asteroid asteroidForCell(int cellX, int cellY, int cellZ) {
        long seed = hash(cellX, cellY, cellZ);
        if (unit(seed) > 0.45) {
            return null;
        }

        double centerX = cellX * CELL_SIZE + 6.0 + unit(seed ^ 0x632BE59BD9B4E019L) * (CELL_SIZE - 12.0);
        double centerY = cellY * VERTICAL_CELL_SIZE + 5.0 + unit(seed ^ 0x85157AF5L) * (VERTICAL_CELL_SIZE - 10.0);
        double centerZ = cellZ * CELL_SIZE + 6.0 + unit(seed ^ 0x9E3779B97F4A7C15L) * (CELL_SIZE - 12.0);

        double radiusX = 3.5 + unit(seed ^ 0xD1B54A32D192ED03L) * 8.5;
        double radiusY = 2.5 + unit(seed ^ 0xABC98388FB8FAC03L) * 7.0;
        double radiusZ = 3.5 + unit(seed ^ 0x8CB92BA72F3D8DD7L) * 8.5;

        return new Asteroid(centerX, centerY, centerZ, radiusX, radiusY, radiusZ);
    }

    private static long hash(int x, int y, int z) {
        long h = 0x9E3779B97F4A7C15L;
        h ^= mix(x * 0x632BE5ABL);
        h ^= mix(y * 0x85157AF5L);
        h ^= mix(z * 0x94D049BBL);
        return mix(h);
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static double unit(long value) {
        return (double) (mix(value) >>> 11) * 0x1.0p-53;
    }

    private record Asteroid(double centerX, double centerY, double centerZ, double radiusX, double radiusY, double radiusZ) {
        boolean contains(int x, int y, int z, SimplexNoise shapeNoise) {
            double dx = (x - centerX) / radiusX;
            double dy = (y - centerY) / radiusY;
            double dz = (z - centerZ) / radiusZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance > 1.25) {
                return false;
            }

            double edgeNoise = shapeNoise.getValue(x * 0.18, y * 0.18, z * 0.18);
            double threshold = 0.82 + edgeNoise * 0.18;
            return distance <= threshold;
        }
    }
}
