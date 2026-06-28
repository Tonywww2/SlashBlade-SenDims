package com.tonywww.slashblade_sendims.mixin.twilightforest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import twilightforest.init.custom.Restrictions;
import twilightforest.util.LandmarkUtil;
import twilightforest.util.LegacyLandmarkPlacements;
import twilightforest.util.WorldUtil;
import twilightforest.world.TFTeleporter;
import twilightforest.world.registration.TFGenerationSettings;

import java.util.Optional;

/**
 * 修复「暮色森林传送门在非法群系生成」的问题。
 *
 * <p>根本原因：暮色森林的群系是<b>按高度分层的三维群系</b>。
 * {@code TFBiomeProvider#getNoiseBiome(x, y, z)} 实际是
 * {@code biomeList.get(getBiome(x, z)).getBiome(quartY)}，其中
 * {@code TerrainColumn#getBiome(elevation)} 会根据 quartY 在同一列里挑选
 * 最接近该高度的群系——也就是说同一 (x, z) 在不同 Y 会得到不同群系
 * （进度限制群系如 Thornlands / Final Plateau 等正是高海拔层）。
 *
 * <p>而原版 {@code TFTeleporter} 的安全检查（{@code isSafe}/{@code checkBiome}）
 * 采样群系用的是<b>传入坐标的 Y</b>，即实体来源维度的 Y。当主世界传送门建在高空
 * （~y300，甚至超过暮色最大高度 288）时，检查采样到的是高海拔层的（安全）群系；
 * 但传送门最终是由 {@code makePortal}/{@code findPortalCoords} 放置在<b>地表高度</b>的，
 * 地表层可能是另一个非法群系。两处 Y 不一致，于是「检查通过」却把门建进了非法群系。
 *
 * <p>修复：重写 {@code isSafeAround}，在做群系安全检查时把采样高度换成传送门真正会落到的
 * 地表高度（用 {@link WorldUtil#getBaseHeight} 按世界生成噪声预测，无需加载区块），
 * 使安全检查与实际放置位置的群系保持一致。世界边界与地标结构检查维持原版语义。
 */
@Mixin(TFTeleporter.class)
public abstract class TFTeleporterMixin {

    /** {@code makePortal} 选址时水平方向最多偏移 16 格，安全检查需覆盖同样范围（与原版一致）。 */
    @Unique
    private static final int slashBlade_SenDims$NEIGHBOR_DISTANCE = 16;

    /**
     * 接管 isSafeAround：使用「地表高度」而非来源 Y 来判定群系安全。
     */
    @Inject(
            method = "isSafeAround(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void slashBlade_SenDims$fixBiomeHeightCheck(
            Level world,
            BlockPos pos,
            Entity entity,
            boolean checkProgression,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(slashBlade_SenDims$isSafeAround(world, pos, entity, checkProgression));
    }

    @Unique
    private static boolean slashBlade_SenDims$isSafeAround(Level world, BlockPos pos, Entity entity, boolean checkProgression) {
        if (!slashBlade_SenDims$isSafe(world, pos, entity, checkProgression)) {
            return false;
        }

        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (!slashBlade_SenDims$isSafe(world, pos.relative(facing, slashBlade_SenDims$NEIGHBOR_DISTANCE), entity, checkProgression)) {
                return false;
            }
        }

        return true;
    }

    @Unique
    private static boolean slashBlade_SenDims$isSafe(Level world, BlockPos pos, Entity entity, boolean checkProgression) {
        // 非暮色维度不限制（与原版一致）
        if (!world.dimension().equals(TFGenerationSettings.DIMENSION_KEY)) {
            return true;
        }

        // 世界边界
        if (!world.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }

        // 关键修复：群系安全必须在传送门真正落点（地表高度）处判定，而不是传入的来源 Y。
        // WorldUtil#getBaseHeight 基于世界生成噪声预测地表高度，与 makePortal 的实际放置高度一致，
        // 且不会强制加载/生成区块。
        if (checkProgression) {
            int surfaceY = WorldUtil.getBaseHeight(world, pos.getX(), pos.getZ(), Heightmap.Types.WORLD_SURFACE_WG);
            BlockPos surfacePos = new BlockPos(pos.getX(), surfaceY, pos.getZ());
            if (!Restrictions.isBiomeSafeFor(world.getBiome(surfacePos).value(), entity)) {
                return false;
            }
        }

        // 地标/结构检查（等价于原版 checkStructure，沿用传入坐标）
        return slashBlade_SenDims$checkStructure(world, pos);
    }

    @Unique
    private static boolean slashBlade_SenDims$checkStructure(Level world, BlockPos pos) {
        boolean outsideLandmarkRange = !LegacyLandmarkPlacements.blockNearLandmarkCenter(pos.getX(), pos.getZ(), 5);
        if (!outsideLandmarkRange) {
            return false;
        }

        Optional<StructureStart> possibleNearLandmark = LandmarkUtil.locateNearestLandmarkStart(
                world,
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ())
        );
        return possibleNearLandmark.isEmpty() || possibleNearLandmark.get().getBoundingBox().isInside(pos);
    }
}