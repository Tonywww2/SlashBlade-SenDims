package com.tonywww.slashblade_sendims.mixin.twilightforest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.init.custom.Restrictions;
import twilightforest.util.LandmarkUtil;
import twilightforest.util.LegacyLandmarkPlacements;
import twilightforest.world.TFTeleporter;
import twilightforest.world.registration.TFGenerationSettings;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(TFTeleporter.class)
public abstract class TFTeleporterMixin {

    @Unique
    private static final int EXTENDED_CHECK_DISTANCE = 24;

    /**
     * 拦截 isSafeAround 方法，扩大检查范围
     */
    @Inject(
            method = "isSafeAround(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void expandPortalSafetyCheck(
            Level world,
            BlockPos pos,
            Entity entity,
            boolean checkProgression,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!slashBlade_SenDims$isSafeAround(world, pos, entity, checkProgression)) {
            cir.setReturnValue(false);
            return;
        }

        // 检查扩展范围
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos checkedPos = pos.relative(direction, EXTENDED_CHECK_DISTANCE);
            if (!slashBlade_SenDims$isSafeAround(world, checkedPos, entity, checkProgression)) {
                cir.setReturnValue(false);
                return;
            }
        }

        cir.setReturnValue(true);
    }

    @Unique
    private static boolean slashBlade_SenDims$isSafeAround(Level world, BlockPos pos, Entity entity, boolean checkProgression) {
        if (!slashBlade_SenDims$isSafe(world, pos, entity, checkProgression)) {
            return false;
        } else {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                if (!slashBlade_SenDims$isSafe(world, pos.relative(facing, 16), entity, checkProgression)) {
                    return false;
                }
            }

            return true;
        }
    }

    @Unique
    private static boolean slashBlade_SenDims$isSafe(Level world, BlockPos pos, Entity entity, boolean checkProgression) {
        boolean outsideLandmarkRange = !LegacyLandmarkPlacements.blockNearLandmarkCenter(pos.getX(), pos.getZ(), 5);

        Optional<StructureStart> possibleNearLandmark = LandmarkUtil.locateNearestLandmarkStart(world, SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));

        boolean checkStructure = outsideLandmarkRange && (possibleNearLandmark.isEmpty() || (possibleNearLandmark.get()).getBoundingBox().isInside(pos));

        return !world.dimension().equals(TFGenerationSettings.DIMENSION_KEY)
                || world.getWorldBorder().isWithinBounds(pos)
                && (!checkProgression || Restrictions.isBiomeSafeFor(world.getBiome(pos).value(), entity))
                && checkStructure;
    }

    /**
     * 拦截 findSafeCoords 方法，改进高度选择
     */
    @Inject(
            method = "findSafeCoords(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Z)Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void improvedFindSafeCoords(
            ServerLevel world,
            int range,
            BlockPos pos,
            Entity entity,
            boolean checkProgression,
            CallbackInfoReturnable<BlockPos> cir
    ) {
        int attempts = range / 8;

        for (int x = 0; x < attempts; x++) {
            for (int z = 0; z < attempts; z++) {
                int xCoord = pos.getX() + (x * attempts) - (range / 2);
                int zCoord = pos.getZ() + (z * attempts) - (range / 2);

                // 不使用固定的 Y=100，而是扫描找最佳高度
                BlockPos safePortalPos = slashBlade_SenDims$findBestHeightForPortal(world, xCoord, zCoord, entity, checkProgression);

                if (safePortalPos != null) {
                    cir.setReturnValue(safePortalPos);
                    return;
                }
            }
        }

        cir.setReturnValue(null);
    }

    /**
     * 找到最佳传送门高度
     * 优先选择安全群系中地面较低的位置
     */
    @Unique
    @Nullable
    private static BlockPos slashBlade_SenDims$findBestHeightForPortal(ServerLevel world, int x, int z, Entity entity, boolean checkProgression) {
        int maxHeight = world.getMaxBuildHeight();
        int minHeight = world.getMinBuildHeight();

        // 从顶部往下扫描
        for (int y = Math.min(maxHeight - 1, 120); y >= minHeight + 10; y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockPos groundPos = new BlockPos(x, y - 1, z);

            // 检查该位置及周围是否安全
            if (TFTeleporter.isSafeAround(world, checkPos, entity, checkProgression)) {
                // 检查下方是否是固体（防止传送到悬崖边上）
                if (world.getBlockState(groundPos).isSolid()) {
                    return checkPos;
                }
            }
        }

        return null;
    }
}