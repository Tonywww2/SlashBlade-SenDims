package com.tonywww.slashblade_sendims.twilight;

import com.tonywww.slashblade_sendims.entities.boss.TwilightBladeWarden;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Queries for KubeJS (the warden ritual in kubejs/server_scripts/custom_spawn/spawn_struct.js).
 * Static methods with primitive arguments only, no overloads.
 */
public final class FinalCastleApi {
    private FinalCastleApi() {
    }

    /** Whether the block is on the open platform at the top centre of a Final Castle. */
    public static boolean isOnSummit(Level level, int x, int y, int z) {
        return level instanceof ServerLevel serverLevel && FinalCastleLocator.isOnSummit(serverLevel, new BlockPos(x, y, z));
    }

    /** Whether a living Twilight Blade Warden is within radius blocks. */
    public static boolean hasWardenNear(Level level, int x, int y, int z, int radius) {
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius + 1, y + radius + 1, z + radius + 1);
        return !level.getEntitiesOfClass(TwilightBladeWarden.class, box, LivingEntity::isAlive).isEmpty();
    }
}
