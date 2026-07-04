package com.tonywww.slashblade_sendims.mixin.twilightforest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
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
import java.util.function.Predicate;

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
 * <p>更隐蔽的是，{@code findPortalCoords} 还用 {@code loc.y * yFactor} 作为选点的目标高度。
 * 来源 Y 为 ~300、暮色侧 {@code yFactor} 为 0.5 时，目标高度被算成 ~150，远高于暮色地表，
 * 于是它在 ±16 范围内会<b>专挑最高的那一列地块</b>，很容易从已校验的安全列漂移到旁边
 * 更高的那一列——也就可能漂进黑森林等非法群系（仅在高空传送时触发）。
 *
 * <p>修复分两部分：
 * <ol>
 *   <li><b>群系检查高度</b>：重写 {@code isSafeAround}，把群系安全检查的采样高度换成传送门真正
 *       会落到的地表高度（用 {@link WorldUtil#getBaseHeight} 按世界生成噪声预测，无需加载区块），
 *       使「是否安全」的判定与实际放置位置一致；
 *   <li><b>放置选点漂移</b>：在暮色侧重写 {@code findPortalCoords}，改为「在地表上挑离中心最近的
 *       合法落点」，不再按来源高度加权，消除高空传送带来的漂移。
 * </ol>
 * 两者配合后，安全检查选出的安全列与真正建门的列保持一致。世界边界与地标结构检查维持原版语义。
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

    /**
     * 接管暮色侧的 findPortalCoords。原版用 {@code loc.y * yFactor}（来源 Y 越高目标越高）作为选点的
     * 目标高度，从高空传送时目标被算到 ~150，使它在 ±16 内总挑最高的地块、落进旁边的非法群系。
     * 这里改为在地表上选「离中心最近的合法落点」，与上面按地表判定群系的逻辑保持一致。
     *
     * <p>仅接管暮色目的地；返程门（主世界侧）无群系限制，保持原版逻辑。
     */
    @Inject(
            method = "findPortalCoords(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;)Lnet/minecraft/core/BlockPos;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void slashBlade_SenDims$pinPortalToSurface(
            ServerLevel world,
            Vec3 loc,
            Predicate<BlockPos> predicate,
            CallbackInfoReturnable<BlockPos> cir
    ) {
        if (!world.dimension().equals(TFGenerationSettings.DIMENSION_KEY)) {
            return;
        }
        cir.setReturnValue(slashBlade_SenDims$findSurfacePortalSpot(world, loc, predicate));
    }

    /**
     * 在以 (loc.x, loc.z) 为中心、半径 16 的范围内，挑选「地表上满足放置条件、且水平距离中心最近」的落点。
     * 不再按来源高度加权，因此不会因为来源在高空而漂移到旁边的列。
     */
    @Unique
    private static BlockPos slashBlade_SenDims$findSurfacePortalSpot(ServerLevel world, Vec3 loc, Predicate<BlockPos> predicate) {
        int centerX = Mth.floor(loc.x);
        int centerZ = Mth.floor(loc.z);
        int range = 16;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestWeight = -1.0D;

        for (int x = centerX - range; x <= centerX + range; x++) {
            double dx = (x + 0.5D) - loc.x;
            for (int z = centerZ - range; z <= centerZ + range; z++) {
                double dz = (z + 0.5D) - loc.z;
                double weight = dx * dx + dz * dz;

                // 已有更近的合法落点时，省去这一列的地表/方块判定
                if (bestWeight >= 0.0D && weight >= bestWeight) {
                    continue;
                }

                // getBaseHeight 返回地表上方第一个空气格，减 1 即顶层实心地块（与原版选点一致）
                int groundY = WorldUtil.getBaseHeight(world, x, z, Heightmap.Types.WORLD_SURFACE_WG) - 1;
                cursor.set(x, groundY, z);
                if (predicate.test(cursor)) {
                    bestWeight = weight;
                    best = cursor.immutable();
                }
            }
        }

        return best;
    }
}