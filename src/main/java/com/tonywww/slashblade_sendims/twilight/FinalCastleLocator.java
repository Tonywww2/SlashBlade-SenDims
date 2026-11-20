package com.tonywww.slashblade_sendims.twilight;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import twilightforest.init.TFLandmark;
import twilightforest.util.LegacyLandmarkPlacements;
import twilightforest.world.registration.TFGenerationSettings;

import javax.annotation.Nullable;

/**
 * Geometry of the Twilight Forest Final Castle, derived the same way the structure places itself.
 *
 * <p>The landmark grid has one landmark per 256x256 region. A region is a Final Castle when the biome at its
 * grid point is the Final Plateau ({@link LegacyLandmarkPlacements#pickLandmarkForChunk}). The castle's main
 * hall ({@code FinalCastleMainComponent}) is centred on the grid point (the landmark centre rounded to a multiple
 * of 256) and spans 48x40x48 blocks starting at sea level + 120.</p>
 */
public final class FinalCastleLocator {
    public static final int REGION = 256;
    public static final int MAIN_HALF = 24;
    public static final int MAIN_FLOOR_OFFSET = 120;
    public static final int MAIN_HEIGHT = 40;
    /** The roof gazebo is 21x21; it starts two blocks above the main hall. */
    public static final int SUMMIT_HALF = 10;
    public static final int SUMMIT_BOTTOM = MAIN_HEIGHT + 2;
    public static final int SUMMIT_TOP = MAIN_HEIGHT + 14;

    private FinalCastleLocator() {
    }

    public static boolean isTwilight(Level level) {
        return level.dimension().equals(TFGenerationSettings.DIMENSION_KEY);
    }

    /** Whether the region whose grid point is (gridX, gridZ) (multiples of 256) holds a Final Castle. */
    public static boolean isCastleAt(WorldGenLevel level, int gridX, int gridZ) {
        return LegacyLandmarkPlacements.pickLandmarkForChunk(gridX >> 4, gridZ >> 4, level) == TFLandmark.FINAL_CASTLE;
    }

    /** Grid coordinate (multiple of 256) of the region containing the block coordinate. */
    public static int gridOf(int block) {
        return Math.floorDiv(block + REGION / 2, REGION) * REGION;
    }

    public static int floorY(ServerLevel level) {
        return level.getChunkSource().getGenerator().getSeaLevel() + MAIN_FLOOR_OFFSET;
    }

    /** Centre (y = main hall floor) of the Final Castle closest to (x, z), searching up to maxRegions regions away. */
    @Nullable
    public static BlockPos findNearestCastle(ServerLevel level, int x, int z, int maxRegions) {
        int ci = Math.floorDiv(x + REGION / 2, REGION);
        int cj = Math.floorDiv(z + REGION / 2, REGION);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int r = 0; r <= maxRegions; r++) {
            if (best != null && (double) (r - 1) * REGION > Math.sqrt(bestDist)) {
                break;
            }
            for (int i = ci - r; i <= ci + r; i++) {
                for (int j = cj - r; j <= cj + r; j++) {
                    if (Math.max(Math.abs(i - ci), Math.abs(j - cj)) != r) {
                        continue;
                    }
                    int gx = i * REGION;
                    int gz = j * REGION;
                    if (!isCastleAt(level, gx, gz)) {
                        continue;
                    }
                    double dx = gx - x;
                    double dz = gz - z;
                    double dist = dx * dx + dz * dz;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = new BlockPos(gx, floorY(level), gz);
                    }
                }
            }
        }
        return best;
    }

    /** Centre of the castle whose region contains pos, or null when that region has no Final Castle. */
    @Nullable
    public static BlockPos castleContaining(ServerLevel level, BlockPos pos) {
        int gx = gridOf(pos.getX());
        int gz = gridOf(pos.getZ());
        return isCastleAt(level, gx, gz) ? new BlockPos(gx, floorY(level), gz) : null;
    }

    public static boolean isInMainHall(ServerLevel level, BlockPos pos) {
        if (!isTwilight(level)) {
            return false;
        }
        BlockPos center = castleContaining(level, pos);
        return center != null
                && Math.abs(pos.getX() - center.getX()) <= MAIN_HALF
                && Math.abs(pos.getZ() - center.getZ()) <= MAIN_HALF
                && pos.getY() >= center.getY() && pos.getY() <= center.getY() + MAIN_HEIGHT;
    }

    /** Whether pos is on the open platform that replaces the roof gazebo, where the warden ritual is held. */
    public static boolean isOnSummit(ServerLevel level, BlockPos pos) {
        if (!isTwilight(level)) {
            return false;
        }
        BlockPos center = castleContaining(level, pos);
        return center != null
                && Math.abs(pos.getX() - center.getX()) <= SUMMIT_HALF
                && Math.abs(pos.getZ() - center.getZ()) <= SUMMIT_HALF
                && pos.getY() >= center.getY() + SUMMIT_BOTTOM && pos.getY() <= center.getY() + SUMMIT_TOP;
    }
}
