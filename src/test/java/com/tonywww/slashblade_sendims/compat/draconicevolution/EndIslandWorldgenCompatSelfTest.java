package com.tonywww.slashblade_sendims.compat.draconicevolution;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class EndIslandWorldgenCompatSelfTest {
    private EndIslandWorldgenCompatSelfTest() {
    }

        public static void main(String[] args) {
        long[] seeds = {0L, 1L, 42L, -1L, Long.MAX_VALUE};
        int[] coordinates = {
                -20000, -5001, -5000, -1025, -1024, -1001, -1000, -999,
                -129, -128, -127, -1, 0, 1, 127, 128, 129,
                999, 1000, 1001, 1024, 1025, 5000, 5001, 20000
        };

        int comparisons = 0;
        for (long seed : seeds) {
            SimplexNoise noise = new SimplexNoise(new LegacyRandomSource(seed));
            for (int x : coordinates) {
                for (int z : coordinates) {
                    float expected = referenceVanillaHeightValue(noise, x, z);
                    float actual = EndIslandWorldgenCompat.getVanillaHeightValue(noise, x, z);
                    if (Float.floatToIntBits(expected) != Float.floatToIntBits(actual)) {
                        throw new AssertionError(
                                "height mismatch at seed=" + seed + ", x=" + x + ", z=" + z
                                        + ": expected=" + expected + ", actual=" + actual
                        );
                    }
                    comparisons++;
                }
            }
        }

        System.out.println("PASS: " + comparisons + " vanilla End island height comparisons");
    }

    private static float referenceVanillaHeightValue(SimplexNoise noise, int x, int z) {
        int regionX = x / 2;
        int regionZ = z / 2;
        int offsetX = x % 2;
        int offsetZ = z % 2;
        float height = Mth.clamp(100.0F - Mth.sqrt((float) (x * x + z * z)) * 8.0F, -100.0F, 80.0F);

        for (int deltaX = -12; deltaX <= 12; deltaX++) {
            for (int deltaZ = -12; deltaZ <= 12; deltaZ++) {
                long islandX = regionX + deltaX;
                long islandZ = regionZ + deltaZ;
                if (islandX * islandX + islandZ * islandZ > 4096L
                        && noise.getValue(islandX, islandZ) < -0.8999999761581421D) {
                    float scale = (Mth.abs((float) islandX) * 3439.0F
                            + Mth.abs((float) islandZ) * 147.0F) % 13.0F + 9.0F;
                    float sampleX = offsetX - deltaX * 2;
                    float sampleZ = offsetZ - deltaZ * 2;
                    float islandHeight = Mth.clamp(
                            100.0F - Mth.sqrt(sampleX * sampleX + sampleZ * sampleZ) * scale,
                            -100.0F,
                            80.0F
                    );
                    height = Math.max(height, islandHeight);
                }
            }
        }

        return height;
    }
}