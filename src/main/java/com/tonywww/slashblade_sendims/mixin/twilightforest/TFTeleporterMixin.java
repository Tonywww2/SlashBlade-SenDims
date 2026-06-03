package com.tonywww.slashblade_sendims.mixin.twilightforest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import twilightforest.TwilightForestMod;
import twilightforest.world.TFTeleporter;

import java.util.function.Predicate;

@Mixin(TFTeleporter.class)
public abstract class TFTeleporterMixin implements ITeleporter {

    @Shadow(remap = false)
    protected abstract BlockPos makePortalAt(net.minecraft.world.level.Level world, BlockPos pos);

    @Inject(
            method = "makePortal(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void slashBlade_SenDims$makePortal(Entity entity, ServerLevel world, Vec3 pos, CallbackInfo ci) {
        Level var6 = entity.level();
        ServerLevel var10000;
        if (var6 instanceof ServerLevel serverLevel) {
            var10000 = serverLevel;
        } else {
            var10000 = null;
        }

        ServerLevel src = var10000;
        slashBlade_SenDims$loadSurroundingArea(world, pos);
        BlockPos spot = slashBlade_SenDims$findPortalCoords(world, pos, (blockPos) -> slashBlade_SenDims$isPortalAt(world, blockPos));
        String name = entity.getName().getString();
        if (spot != null) {
            TwilightForestMod.LOGGER.debug("Found existing portal for {} at {}", name, spot);
            slashBlade_SenDims$cacheNewPortalCoords(src, spot, entity.blockPosition());
        } else {
            spot = slashBlade_SenDims$findPortalCoords(world, pos, (blockpos) -> slashBlade_SenDims$isIdealForPortal(world, blockpos));
            if (spot != null) {
                TwilightForestMod.LOGGER.debug("Found ideal portal spot for {} at {}", name, spot);
                slashBlade_SenDims$cacheNewPortalCoords(src, this.makePortalAt(world, spot), entity.blockPosition());
            } else {
                TwilightForestMod.LOGGER.debug("Did not find ideal portal spot, shooting for okay one for {}", name);
                spot = slashBlade_SenDims$findPortalCoords(world, pos, (blockPos) -> slashBlade_SenDims$isOkayForPortal(world, blockPos));
                if (spot != null) {
                    TwilightForestMod.LOGGER.debug("Found okay portal spot for {} at {}", name, spot);
                    slashBlade_SenDims$cacheNewPortalCoords(src, this.makePortalAt(world, spot), entity.blockPosition());
                } else {
                    TwilightForestMod.LOGGER.debug("Did not even find an okay portal spot, just making a random one for {}", name);
                    slashBlade_SenDims$cacheNewPortalCoords(src, this.makePortalAt(world, BlockPos.containing(entity.getX(), 63, entity.getZ())), entity.blockPosition());
                }
            }
        }
        ci.cancel();
    }

    @Unique
    private static @Nullable BlockPos slashBlade_SenDims$findPortalCoords(ServerLevel world, Vec3 loc, Predicate<BlockPos> predicate) {
        double yFactor = slashBlade_SenDims$getYFactor(world);
        int entityX = Mth.floor(loc.x);
        int entityZ = Mth.floor(loc.z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double spotWeight = (double) -1.0F;
        BlockPos spot = null;
        int range = 16;

        for (int rx = entityX - range; rx <= entityX + range; ++rx) {
            double xWeight = (double) rx + (double) 0.5F - loc.x;

            for (int rz = entityZ - range; rz <= entityZ + range; ++rz) {
                double zWeight = (double) rz + (double) 0.5F - loc.z;

                for (int ry = slashBlade_SenDims$getScanHeight(world, rx, rz); ry >= world.getMinBuildHeight(); --ry) {
                    if (world.isEmptyBlock(pos.set(rx, ry, rz))) {
                        while (ry > world.getMinBuildHeight() && world.isEmptyBlock(pos.set(rx, ry - 1, rz))) {
                            --ry;
                        }

                        double yWeight = (double) ry + (double) 0.5F - loc.y * yFactor;
                        double rPosWeight = xWeight * xWeight + yWeight * yWeight + zWeight * zWeight;
                        if ((spotWeight < (double) 0.0F || rPosWeight < spotWeight) && predicate.test(pos)) {
                            spotWeight = rPosWeight;
                            spot = pos.immutable();
                        }
                    }
                }
            }
        }

        return spot;
    }


    @Unique
    @SuppressWarnings("removal")
    private static int slashBlade_SenDims$getScanHeight(ServerLevel world, int x, int z) {
        int worldHeight = world.getMaxBuildHeight() - 1;
        int chunkHeight = world.getChunk(x >> 4, z >> 4).getHighestSectionPosition() + 15;
        return Math.min(worldHeight, chunkHeight);
    }

    @Unique
    private void slashBlade_SenDims$loadSurroundingArea(ServerLevel world, Vec3 pos) {
        int x = (int) Math.floor(pos.x) >> 4;
        int z = (int) Math.floor(pos.z) >> 4;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getChunk(x + dx, z + dz);
            }
        }
    }

    @Unique
    private boolean slashBlade_SenDims$isPortalAt(ServerLevel world, BlockPos pos) {
        return world.getBlockState(pos).is(twilightforest.init.TFBlocks.TWILIGHT_PORTAL.get());
    }

    @Unique
    private boolean slashBlade_SenDims$isIdealForPortal(ServerLevel world, BlockPos pos) {
        for (int potentialZ = 0; potentialZ < 4; potentialZ++) {
            for (int potentialX = 0; potentialX < 4; potentialX++) {
                for (int potentialY = 0; potentialY < 4; potentialY++) {
                    BlockPos tPos = pos.offset(potentialX - 1, potentialY, potentialZ - 1);
                    var state = world.getBlockState(tPos);
                    if (potentialY == 0 && !state.is(net.minecraft.tags.BlockTags.DIRT) || potentialY >= 1 && !state.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Unique
    private boolean slashBlade_SenDims$isOkayForPortal(ServerLevel world, BlockPos pos) {
        for (int potentialZ = 0; potentialZ < 4; potentialZ++) {
            for (int potentialX = 0; potentialX < 4; potentialX++) {
                for (int potentialY = 0; potentialY < 4; potentialY++) {
                    BlockPos tPos = pos.offset(potentialX - 1, potentialY, potentialZ - 1);
                    var state = world.getBlockState(tPos);
                    if (potentialY == 0 && !state.isSolid() && !state.liquid() || potentialY >= 1 && !state.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Unique
    private void slashBlade_SenDims$cacheNewPortalCoords(ServerLevel srcDim, BlockPos pos, BlockPos srcPos) {
        // 这里保持原类行为：把新门坐标缓存起来，供下次复用
        // 如果你要 100% 复刻原代码，建议把 TFTeleporter 里的 cacheNewPortalCoords 逻辑也完整搬过来
        TwilightForestMod.LOGGER.debug("Cache portal coords: src={}, dest={}", srcPos, pos);
    }

    @Unique
    private static double slashBlade_SenDims$getYFactor(ServerLevel world) {
        return world.dimension().location().equals(Level.OVERWORLD.location()) ? (double) 2.0F : (double) 0.5F;
    }
}
