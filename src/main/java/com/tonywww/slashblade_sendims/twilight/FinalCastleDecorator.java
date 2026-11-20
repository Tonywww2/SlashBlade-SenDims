package com.tonywww.slashblade_sendims.twilight;

import com.tonywww.slashblade_sendims.mixin.minecraft.StructurePieceWorldPosInvoker;
import com.tonywww.slashblade_sendims.mixin.twilightforest.TowerWingComponentAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import twilightforest.init.TFBlocks;
import twilightforest.init.TFEntities;
import twilightforest.world.components.structures.finalcastle.FinalCastleEntranceTowerComponent;
import twilightforest.world.components.structures.finalcastle.FinalCastleMazeTower13Component;

import java.util.List;

/**
 * World generation additions to the Final Castle, called at the end of each piece's postProcess
 * (mixins in {@code mixin.twilightforest.finalcastle}). Coordinates are piece-local, so they follow the piece's
 * orientation. Everything writes only inside the chunk being generated (sbb), so each block and each guard is
 * placed exactly once.
 *
 * <p>Layout: modpack docs/development/暮色森林流程重做-调查与设计-2026-11-18.md 6.2 and 7.2, contents:
 * 终焉城堡内容设计-2026-11-18.md.</p>
 */
public final class FinalCastleDecorator {
    public static final String GUARD_TAG = "sdbf_castle_guard";
    private static final List<BlockState> PORTAL_DECO = List.of(
            Blocks.POPPY.defaultBlockState(), Blocks.DANDELION.defaultBlockState(),
            Blocks.GRASS.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState());

    private FinalCastleDecorator() {
    }

    // ---------- pieces ----------

    /** Main hall: portal on the west side of the first floor, lights around it, supply chests, mezzanine chest. */
    public static void decorateMain(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState portal = TFBlocks.TWILIGHT_PORTAL.get().defaultBlockState();
        for (int x = 4; x <= 7; x++) {
            for (int z = 23; z <= 26; z++) {
                boolean inner = x >= 5 && x <= 6 && z >= 24 && z <= 25;
                set(world, sbb, piece, x, 1, z, inner ? portal : grass);
                set(world, sbb, piece, x, 2, z, inner ? Blocks.AIR.defaultBlockState() : PORTAL_DECO.get(Math.floorMod(x * 7 + z * 3, PORTAL_DECO.size())));
            }
        }
        BlockState lantern = Blocks.LANTERN.defaultBlockState();
        set(world, sbb, piece, 4, 1, 21, lantern);
        set(world, sbb, piece, 8, 1, 21, lantern);
        set(world, sbb, piece, 4, 1, 28, lantern);
        set(world, sbb, piece, 8, 1, 28, lantern);

        chest(world, sbb, piece, 2, 1, 19, "supply", false);
        chest(world, sbb, piece, 2, 1, 29, "supply", false);
        chest(world, sbb, piece, 14, 31, 2, "tower", false);
    }

    /** Roof: violet key chest and its guard; force field on the parapet so the warden fight cannot knock players off. */
    public static void decorateRoof(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        BlockState field = TFBlocks.VIOLET_FORCE_FIELD.get().defaultBlockState();
        // The parapet runs one block in from the roof edge (roof local 1 and 51). The corner towers stand on it.
        for (int along = 0; along <= 52; along++) {
            for (int y = 1; y <= 5; y++) {
                if (!inCornerTower(along, 1)) setIfAir(world, sbb, piece, along, y, 1, field);
                if (!inCornerTower(along, 51)) setIfAir(world, sbb, piece, along, y, 51, field);
                if (!inCornerTower(1, along)) setIfAir(world, sbb, piece, 1, y, along, field);
                if (!inCornerTower(51, along)) setIfAir(world, sbb, piece, 51, y, along, field);
            }
        }
        chest(world, sbb, piece, 10, 3, 10, "key_violet", false);
        guard(world, sbb, piece, 14, 3, 14, TFEntities.ADHERENT.get());
    }

    /** Large tower ("Parkour area 1" placeholder): chest where the sign was. */
    public static void decorateLargeTower(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        chest(world, sbb, piece, 6, 1, 6, "tower", true);
    }

    /** Yellow/blue maze towers and the entrance tower (end of the yellow route). */
    public static void decorateMazeTower(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        Class<?> type = piece.getClass();
        if (type == FinalCastleMazeTower13Component.class) {
            cornerChest(piece, world, sbb, "tower");
        } else if (type == FinalCastleEntranceTowerComponent.class) {
            int y = entranceY(piece);
            cornerChest(piece, world, sbb, "key_yellow");
            guard(world, sbb, piece, 8, y, 8, TFEntities.REDCAP.get());
        }
    }

    /** Bell tower ("Parkour area 2 / mini-boss 1" placeholder, end of the blue route). */
    public static void decorateBellTower(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        chest(world, sbb, piece, 7, 9, 8, "key_blue", true);
        guard(world, sbb, piece, 10, 9, 10, TFEntities.WRAITH.get());
    }

    /** Damaged and wrecked towers: dead ends full of thorns. */
    public static void decorateDamagedTower(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        cornerChest(piece, world, sbb, "thorn_tower");
    }

    /** Ordinary dungeon room: half of them get a chest in one outer corner, outside the force field cage. */
    public static void decorateDungeonRoom(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        BoundingBox box = piece.getBoundingBox();
        long hash = Mth.getSeed(box.minX(), box.minY(), box.minZ());
        if ((hash & 1L) == 0L) {
            return;
        }
        int corner = (int) ((hash >>> 1) & 3L);
        int x = (corner & 1) == 0 ? 1 : 29;
        int z = (corner & 2) == 0 ? 1 : 29;
        chest(world, sbb, piece, x, 0, z, "dungeon", false);
    }

    public static void decorateDungeonEntrance(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        chest(world, sbb, piece, 9, 0, 9, "dungeon", false);
    }

    public static void decorateDungeonExit(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        chest(world, sbb, piece, 21, 0, 21, "dungeon", false);
    }

    /** Forge room at the bottom of the dungeon ("Mini-boss 2" placeholder, end of the magenta route). */
    public static void decorateForgeRoom(StructurePiece piece, WorldGenLevel world, BoundingBox sbb) {
        chest(world, sbb, piece, 25, 0, 25, "key_pink", true);
        guard(world, sbb, piece, 25, 0, 20, TFEntities.FIRE_BEETLE.get());
    }

    // ---------- helpers ----------

    /**
     * Roof local coordinates (main hall local + 2) covered by a corner tower, plus one block: stair towers are 9 wide,
     * the large tower (main hall corner maxX/minZ) is 13 wide. Inside them the "air" is the tower's own stairwell.
     */
    private static boolean inCornerTower(int rx, int rz) {
        boolean east = rx > 26;
        boolean north = rz < 26;
        int half = east && north ? 7 : 5;
        int cx = east ? 50 : 2;
        int cz = north ? 2 : 50;
        return Math.abs(rx - cx) <= half && Math.abs(rz - cz) <= half;
    }

    private static int entranceY(StructurePiece piece) {
        List<BlockPos> openings = ((TowerWingComponentAccessor) piece).sendims$getOpenings();
        return openings.isEmpty() ? 1 : openings.get(0).getY();
    }

    /** Corner of the floor the tower was entered on, farthest from that door (the door is on local x = 0). */
    private static void cornerChest(StructurePiece piece, WorldGenLevel world, BoundingBox sbb, String table) {
        int y = entranceY(piece);
        if (!chest(world, sbb, piece, 11, y, 11, table, false)) {
            chest(world, sbb, piece, 11, y, 1, table, false);
        }
    }

    private static BlockPos worldPos(StructurePiece piece, int x, int y, int z) {
        StructurePieceWorldPosInvoker invoker = (StructurePieceWorldPosInvoker) piece;
        return new BlockPos(invoker.sendims$getWorldX(x, z), invoker.sendims$getWorldY(y), invoker.sendims$getWorldZ(x, z));
    }

    private static void set(WorldGenLevel world, BoundingBox sbb, StructurePiece piece, int x, int y, int z, BlockState state) {
        BlockPos pos = worldPos(piece, x, y, z);
        if (sbb.isInside(pos)) {
            world.setBlock(pos, state, 2);
        }
    }

    private static void setIfAir(WorldGenLevel world, BoundingBox sbb, StructurePiece piece, int x, int y, int z, BlockState state) {
        BlockPos pos = worldPos(piece, x, y, z);
        if (sbb.isInside(pos) && world.getBlockState(pos).isAir()) {
            world.setBlock(pos, state, 2);
        }
    }

    /**
     * Places a loot chest. Without replace the spot must be air with a solid block below; with replace it overwrites
     * whatever is there (used for the placeholder signs).
     *
     * @return whether the position is in this chunk and usable (also true when the chest is placed by another pass)
     */
    private static boolean chest(WorldGenLevel world, BoundingBox sbb, StructurePiece piece, int x, int y, int z, String table, boolean replace) {
        BlockPos pos = worldPos(piece, x, y, z);
        if (!sbb.isInside(pos)) {
            return true;
        }
        if (!replace && (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP))) {
            return false;
        }
        world.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
        RandomizableContainerBlockEntity.setLootTable(world, world.getRandom(), pos, ResourceLocation.fromNamespaceAndPath("sdbf", "chests/final_castle/" + table));
        return true;
    }

    /**
     * Spawns one persistent key guard near the target. Only the chunk containing the target spawns it, and only on a
     * free spot inside that chunk, so a guard is never duplicated. {@link FinalCastleEvents} makes it a Leader.
     */
    private static void guard(WorldGenLevel world, BoundingBox sbb, StructurePiece piece, int x, int y, int z, EntityType<? extends Mob> type) {
        BlockPos target = worldPos(piece, x, y, z);
        if (!sbb.isInside(target)) {
            return;
        }
        BlockPos spot = null;
        search:
        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos pos = target.offset(dx, 0, dz);
                    if (sbb.isInside(pos) && world.getBlockState(pos).isAir() && world.getBlockState(pos.above()).isAir()
                            && world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP)) {
                        spot = pos;
                        break search;
                    }
                }
            }
        }
        if (spot == null) {
            return;
        }
        Mob mob = type.create(world.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, world.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.addTag(GUARD_TAG);
        mob.finalizeSpawn(world, world.getCurrentDifficultyAt(spot), MobSpawnType.STRUCTURE, null, null);
        world.addFreshEntityWithPassengers(mob);
    }
}
