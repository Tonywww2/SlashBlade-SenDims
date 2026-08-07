package com.tonywww.slashblade_sendims.compat.draconicevolution;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

public final class EndIslandWorldgenCompat {
    private EndIslandWorldgenCompat() {
    }

    public static float getVanillaHeightValue(SimplexNoise noise, int x, int z) {
        int regionX = x / 2;
        int regionZ = z / 2;
        int offsetX = x % 2;
        int offsetZ = z % 2;
        float height = Mth.clamp(100.0F - Mth.sqrt((float) (x * x + z * z)) * 8.0F, -100.0F, 80.0F);

        for (int deltaX = -12; deltaX <= 12; deltaX++) {
            for (int deltaZ = -12; deltaZ <= 12; deltaZ++) {
                long islandX = regionX + deltaX;
                long islandZ = regionZ + deltaZ;
                if (islandX * islandX + islandZ * islandZ <= 4096L
                        || noise.getValue(islandX, islandZ) >= -0.9F) {
                    continue;
                }

                float islandScale = (Mth.abs((float) islandX) * 3439.0F
                        + Mth.abs((float) islandZ) * 147.0F) % 13.0F + 9.0F;
                float distanceX = offsetX - deltaX * 2;
                float distanceZ = offsetZ - deltaZ * 2;
                float islandHeight = Mth.clamp(
                        100.0F - Mth.sqrt(distanceX * distanceX + distanceZ * distanceZ) * islandScale,
                        -100.0F,
                        80.0F
                );
                height = Math.max(height, islandHeight);
            }
        }

        return height;
    }
}