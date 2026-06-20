package com.tonywww.slashblade_sendims.worldgen;

import com.tonywww.slashblade_sendims.registeries.SBSDBlocks;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;

public class SaturnRingBlocks {
    public static BlockState BASE_RING_SURFACE = SBSDBlocks.SATURN_SANDSTONE.get().defaultBlockState();
    public static BlockState BASE_RING_SUBSURFACE = SBSDBlocks.SATURN_STONE.get().defaultBlockState();
    public static BlockState BASE_RING_CORE = SBSDBlocks.SATURN_STONE.get().defaultBlockState();

    public static BlockState HIGH_DENSITY_SURFACE = SBSDBlocks.SATURN_COBBLED_DEEPSLATE.get().defaultBlockState();
    public static BlockState HIGH_DENSITY_CORE = SBSDBlocks.SATURN_DEEPSLATE.get().defaultBlockState();

    public static BlockState TRANSITION_WALL_SURFACE = SBSDBlocks.SATURN_COBBLED_DEEPSLATE.get().defaultBlockState();
    public static BlockState TRANSITION_WALL_CORE = SBSDBlocks.SATURN_COBBLESTONE.get().defaultBlockState();

    public static BlockState SPARSE_RING_SURFACE = SBSDBlocks.POROUS_SATURN_STONE.get().defaultBlockState();
    public static BlockState SPARSE_RING_CORE = SBSDBlocks.SATURN_STONE.get().defaultBlockState();
    public static BlockState FADING_RING_SURFACE = SBSDBlocks.POROUS_SATURN_STONE.get().defaultBlockState();
    public static BlockState FADING_RING_CORE = SBSDBlocks.POROUS_SATURN_STONE.get().defaultBlockState();

    public static BlockState getSurfaceState(Holder<Biome> biomeHolder) {
        return getSurfaceState(biomeHolder, 0.0);
    }

    public static BlockState getSurfaceState(Holder<Biome> biomeHolder, double materialNoise) {
        if (biomeHolder.is(SaturnRingBiomes.HIGH_DENSITY_RING)) {
            return materialNoise > 0.35 ? TRANSITION_WALL_SURFACE : HIGH_DENSITY_SURFACE;
        } else if (biomeHolder.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            return materialNoise < -0.35 ? TRANSITION_WALL_CORE : TRANSITION_WALL_SURFACE;
        } else if (biomeHolder.is(SaturnRingBiomes.INNER_SPARSE_RING) || biomeHolder.is(SaturnRingBiomes.OUTER_SPARSE_RING)) {
            return materialNoise < -0.45 ? BASE_RING_SUBSURFACE : SPARSE_RING_SURFACE;
        } else if (biomeHolder.is(SaturnRingBiomes.INNER_FADING_RING)) {
            return materialNoise > 0.45 ? BASE_RING_SUBSURFACE : FADING_RING_SURFACE;
        } else if (biomeHolder.is(SaturnRingBiomes.VOID_RING)) {
            return Blocks.AIR.defaultBlockState();
        }
        return materialNoise > 0.50 ? BASE_RING_SUBSURFACE : BASE_RING_SURFACE;
    }

    public static BlockState getCoreState(Holder<Biome> biomeHolder) {
        return getCoreState(biomeHolder, 0.0);
    }

    public static BlockState getCoreState(Holder<Biome> biomeHolder, double materialNoise) {
        if (biomeHolder.is(SaturnRingBiomes.HIGH_DENSITY_RING)) {
            return materialNoise > 0.45 ? HIGH_DENSITY_SURFACE : HIGH_DENSITY_CORE;
        } else if (biomeHolder.is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            return materialNoise > 0.20 ? TRANSITION_WALL_SURFACE : TRANSITION_WALL_CORE;
        } else if (biomeHolder.is(SaturnRingBiomes.INNER_FADING_RING)) {
            return materialNoise > 0.55 ? BASE_RING_CORE : FADING_RING_CORE;
        } else if (biomeHolder.is(SaturnRingBiomes.INNER_SPARSE_RING) || biomeHolder.is(SaturnRingBiomes.OUTER_SPARSE_RING)) {
            if (materialNoise > 0.35) {
                return SPARSE_RING_SURFACE;
            }
            return materialNoise < -0.55 ? TRANSITION_WALL_CORE : SPARSE_RING_CORE;
        }
        if (materialNoise > 0.55) {
            return BASE_RING_SURFACE;
        }
        return materialNoise < -0.60 ? SPARSE_RING_SURFACE : BASE_RING_CORE;
    }
}
