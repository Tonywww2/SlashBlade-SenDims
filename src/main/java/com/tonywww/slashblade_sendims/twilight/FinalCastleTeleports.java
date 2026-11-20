package com.tonywww.slashblade_sendims.twilight;

import com.tonywww.slashblade_sendims.config.SenDimsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import twilightforest.init.TFBlocks;

import javax.annotation.Nullable;

/**
 * Entry into the Twilight Forest always lands next to the portal in the nearest Final Castle's main hall
 * ({@link FinalCastleDecorator} builds that portal during world generation). Leaving through that portal returns
 * each player to the overworld portal they used, recorded per player.
 */
public final class FinalCastleTeleports {
    private static final String RETURN_KEY = "slashblade_sendims.tf_return";
    /** Castles are about one per 2048x2048 blocks; 16 regions is 4096 blocks. */
    private static final int SEARCH_REGIONS = 16;
    /** Arrival spot, main hall local coordinates: just east of the portal, facing into the hall. */
    private static final double ARRIVAL_LOCAL_X = 9.5;
    private static final double ARRIVAL_LOCAL_Z = 25.0;

    private FinalCastleTeleports() {
    }

    /** Exit through a castle portal; info null means the entity may not use it. */
    public record Exit(@Nullable PortalInfo info) {
    }

    @Nullable
    public static PortalInfo intoCastle(Entity entity, ServerLevel dest) {
        if (!SenDimsConfig.TF_CASTLE_PORTAL.get() || !FinalCastleLocator.isTwilight(dest) || FinalCastleLocator.isTwilight(entity.level())) {
            return null;
        }
        double scale = DimensionType.getTeleportationScale(entity.level().dimensionType(), dest.dimensionType());
        BlockPos target = dest.getWorldBorder().clampToBounds(entity.getX() * scale, entity.getY(), entity.getZ() * scale);
        BlockPos center = FinalCastleLocator.findNearestCastle(dest, target.getX(), target.getZ(), SEARCH_REGIONS);
        if (center == null) {
            return null;
        }
        if (entity instanceof ServerPlayer player) {
            rememberReturn(player);
        }
        double x = center.getX() - FinalCastleLocator.MAIN_HALF + ARRIVAL_LOCAL_X;
        double z = center.getZ() - FinalCastleLocator.MAIN_HALF + ARRIVAL_LOCAL_Z;
        return new PortalInfo(new Vec3(x, center.getY() + 1, z), Vec3.ZERO, -90.0F, entity.getXRot());
    }

    @Nullable
    public static Exit outOfCastle(Entity entity, ServerLevel dest) {
        if (!SenDimsConfig.TF_CASTLE_PORTAL.get() || FinalCastleLocator.isTwilight(dest) || !(entity.level() instanceof ServerLevel from)
                || !FinalCastleLocator.isInMainHall(from, entity.blockPosition())) {
            return null;
        }
        if (!(entity instanceof ServerPlayer player)) {
            // Items and mobs would otherwise make stray portals in the overworld sky.
            return new Exit(null);
        }
        BlockPos back = readReturn(player, dest);
        if (back == null) {
            back = dest.getSharedSpawnPos();
        }
        BlockPos spot = findStandable(dest, back);
        return new Exit(new PortalInfo(Vec3.atBottomCenterOf(spot), Vec3.ZERO, player.getYRot(), player.getXRot()));
    }

    private static void rememberReturn(ServerPlayer player) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag back = new CompoundTag();
        back.putString("dim", player.level().dimension().location().toString());
        back.putInt("x", player.getBlockX());
        back.putInt("y", player.getBlockY());
        back.putInt("z", player.getBlockZ());
        persisted.put(RETURN_KEY, back);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    @Nullable
    private static BlockPos readReturn(ServerPlayer player, ServerLevel dest) {
        CompoundTag back = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(RETURN_KEY);
        ResourceLocation dim = ResourceLocation.tryParse(back.getString("dim"));
        if (dim == null || !dim.equals(dest.dimension().location())) {
            return null;
        }
        return new BlockPos(back.getInt("x"), back.getInt("y"), back.getInt("z"));
    }

    /** A spot next to (not inside) a portal: walking into a Twilight portal sends the entity straight back. */
    private static BlockPos findStandable(ServerLevel level, BlockPos origin) {
        for (int r = 0; r <= 3; r++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                            continue;
                        }
                        BlockPos pos = origin.offset(dx, dy, dz);
                        if (isStandable(level, pos)) {
                            return pos;
                        }
                    }
                }
            }
        }
        return origin.above();
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        return !isPortal(feet) && !isPortal(below)
                && feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean isPortal(BlockState state) {
        return state.is(TFBlocks.TWILIGHT_PORTAL.get());
    }
}
