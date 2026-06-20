package com.tonywww.slashblade_sendims.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SaturnRingChunkGenerator extends ChunkGenerator {

    public static final Codec<SaturnRingChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SaturnRingBiomeSource.CODEC.fieldOf("biome_source").forGetter(cg -> (SaturnRingBiomeSource) cg.getBiomeSource())
    ).apply(instance, SaturnRingChunkGenerator::new));

    private static final int RING_Y_CENTER = 96;

    private final SimplexNoise porosityNoise = new SimplexNoise(new net.minecraft.world.level.levelgen.LegacyRandomSource(114514L));
    private final SimplexNoise heightNoise = new SimplexNoise(new net.minecraft.world.level.levelgen.LegacyRandomSource(223344L));
    private final SimplexNoise thicknessNoise = new SimplexNoise(new LegacyRandomSource(880301L));
    private final SimplexNoise ridgeNoise = new SimplexNoise(new LegacyRandomSource(550771L));
    private final SimplexNoise materialNoise = new SimplexNoise(new LegacyRandomSource(910021L));
    private final SimplexNoise asteroidShapeNoise = new SimplexNoise(new LegacyRandomSource(730021L));

    public SaturnRingChunkGenerator(SaturnRingBiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // Saturn ring terrain does not use vanilla carvers.
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = chunk.getPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getMinBlockX() + x;
                int worldZ = chunkPos.getMinBlockZ() + z;

                Holder<Biome> biomeHolder = this.biomeSource.getNoiseBiome(worldX >> 2, 0, worldZ >> 2, randomState.sampler());
                int maxY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                BlockState surfaceState = SaturnRingBlocks.getSurfaceState(biomeHolder, sampleMaterialNoise(worldX, maxY, worldZ));

                if (maxY > chunk.getMinBuildHeight() && !surfaceState.isAir()) {
                    pos.set(x, maxY, z);
                    BlockState currentBlock = chunk.getBlockState(pos);
                    if (!currentBlock.isAir()) {
                        chunk.setBlockState(pos, surfaceState, false);
                    }
                }
            }
        }
        decorateChunk(chunk, randomState);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public int getGenDepth() {
        return 192;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = minX + localX;
                int worldZ = minZ + localZ;

                SaturnRingTerrainProfile profile = sampleProfile(worldX, worldZ, randomState);

                if (!profile.hasTerrain()) {
                    continue;
                }

                for (int y = profile.bottomY(); y <= profile.topY(); y++) {
                    pos.set(localX, y, localZ);

                    if (!isSolidTerrain(worldX, y, worldZ, profile)) {
                        continue;
                    }

                    chunk.setBlockState(pos, SaturnRingBlocks.getCoreState(profile.biomeHolder(), sampleMaterialNoise(worldX, y, worldZ)), false);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private SaturnRingTerrainProfile sampleProfile(int x, int z, RandomState randomState) {
        return SaturnRingTerrainProfile.sample(x, z, randomState.sampler(), this.biomeSource, heightNoise, thicknessNoise, RING_Y_CENTER);
    }

    private boolean isSolidTerrain(int x, int y, int z, SaturnRingTerrainProfile profile) {
        if (profile.biomeHolder().is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            return profile.containsY(y)
                    && (SaturnRingAsteroidField.isInsideTransitionBridge(x, y, z, RING_Y_CENTER, asteroidShapeNoise)
                    || SaturnRingAsteroidField.isInsideAsteroid(x, y, z, asteroidShapeNoise));
        }
        return profile.containsY(y) && !isPorous(x, y, z, profile.biomeHolder());
    }

    private double sampleMaterialNoise(int x, int y, int z) {
        double broadPatch = materialNoise.getValue(x * 0.025, z * 0.025);
        double verticalPatch = materialNoise.getValue(x * 0.055 + 700.0, y * 0.045, z * 0.055 - 700.0) * 0.45;
        return broadPatch + verticalPatch;
    }

    private boolean isPorous(int x, int y, int z, Holder<Biome> biome) {
        if (biome.is(SaturnRingBiomes.INNER_FADING_RING)) {
            double fadeRatio = (Math.abs(z) - 6000.0) / 4000.0;
            fadeRatio = Math.max(0.0, Math.min(1.0, fadeRatio));
            double noiseVal = porosityNoise.getValue(x * 0.03, y * 0.08, z * 0.03);
            double threshold = -0.8 + (1.6 * fadeRatio);
            return noiseVal < threshold;
        } else if (biome.is(SaturnRingBiomes.INNER_SPARSE_RING)) {
            double noiseVal = porosityNoise.getValue(x * 0.05, y * 0.1, z * 0.05);
            return noiseVal < -0.4;
        } else if (biome.is(SaturnRingBiomes.HIGH_DENSITY_RING)) {
            double crackNoise = porosityNoise.getValue(x * 0.01, y * 0.01, z * 0.05);
            double ridge = Math.abs(ridgeNoise.getValue(x * 0.006, z * 0.030));
            return crackNoise > 0.6 || ridge < 0.08;
        }

        return false;
    }

    private void decorateChunk(ChunkAccess chunk, RandomState randomState) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = chunkPos.getMinBlockX() + localX;
                int worldZ = chunkPos.getMinBlockZ() + localZ;
                SaturnRingTerrainProfile profile = sampleProfile(worldX, worldZ, randomState);

                if (!profile.hasTerrain()) {
                    continue;
                }

                int surfaceY = findSurfaceY(chunk, localX, localZ, profile);
                if (surfaceY != Integer.MIN_VALUE) {
                    placeSurfaceRubble(chunk, pos, localX, surfaceY, localZ, worldX, worldZ, profile);
                    placeRockSpike(chunk, pos, localX, surfaceY, localZ, worldX, worldZ, profile);
                    placeGlowPoint(chunk, pos, localX, surfaceY, localZ, worldX, worldZ, profile);
                }

                placeFloatingFragment(chunk, pos, localX, localZ, worldX, worldZ, profile);
            }
        }
    }

    private int findSurfaceY(ChunkAccess chunk, int localX, int localZ, SaturnRingTerrainProfile profile) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(profile.bottomY(), chunk.getMinBuildHeight());
        int maxY = Math.min(profile.topY(), chunk.getMaxBuildHeight() - 2);

        for (int y = maxY; y >= minY; y--) {
            pos.set(localX, y, localZ);
            if (!chunk.getBlockState(pos).isAir()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private void placeSurfaceRubble(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int localX, int surfaceY,
                                    int localZ, int worldX, int worldZ, SaturnRingTerrainProfile profile) {
        if (profile.biomeHolder().is(SaturnRingBiomes.VOID_RING) || chance(worldX, surfaceY, worldZ, 0x3A91L) > 0.08) {
            return;
        }

        BlockState state = SaturnRingBlocks.getSurfaceState(profile.biomeHolder(), sampleMaterialNoise(worldX, surfaceY + 1, worldZ));
        setIfAir(chunk, pos, localX, surfaceY + 1, localZ, state);

        if (chance(worldX, surfaceY, worldZ, 0x64C3L) < 0.35) {
            int dx = signedOffset(worldX, worldZ, 0x18B1L);
            int dz = signedOffset(worldX, worldZ, 0x791DL);
            setIfAir(chunk, pos, localX + dx, surfaceY + 1, localZ + dz, state);
        }
    }

    private void placeRockSpike(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int localX, int surfaceY,
                                int localZ, int worldX, int worldZ, SaturnRingTerrainProfile profile) {
        double chance = profile.biomeHolder().is(SaturnRingBiomes.HIGH_DENSITY_RING) ? 0.030 : 0.012;
        if (profile.biomeHolder().is(SaturnRingBiomes.INNER_SPARSE_RING) || profile.biomeHolder().is(SaturnRingBiomes.OUTER_SPARSE_RING)) {
            chance = 0.020;
        } else if (profile.biomeHolder().is(SaturnRingBiomes.INNER_FADING_RING) || profile.biomeHolder().is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            chance = 0.006;
        }

        if (chance(worldX, surfaceY, worldZ, 0x5477L) > chance) {
            return;
        }

        int height = 2 + (int) Math.floor(chance(worldX, surfaceY, worldZ, 0x2EA1L) * 3.0);
        BlockState state = SaturnRingBlocks.getCoreState(profile.biomeHolder(), sampleMaterialNoise(worldX, surfaceY, worldZ));
        for (int yOffset = 1; yOffset <= height; yOffset++) {
            setIfAir(chunk, pos, localX, surfaceY + yOffset, localZ, state);
        }
    }

    private void placeGlowPoint(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int localX, int surfaceY,
                                int localZ, int worldX, int worldZ, SaturnRingTerrainProfile profile) {
        if (!profile.biomeHolder().is(SaturnRingBiomes.HIGH_DENSITY_RING) && !profile.biomeHolder().is(SaturnRingBiomes.TRANSITION_WALL_RING)) {
            return;
        }
        if (chance(worldX, surfaceY, worldZ, 0x7CC5L) > 0.006) {
            return;
        }
        setIfAir(chunk, pos, localX, surfaceY + 1, localZ, Blocks.SEA_LANTERN.defaultBlockState());
    }

    private void placeFloatingFragment(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int localX, int localZ,
                                       int worldX, int worldZ, SaturnRingTerrainProfile profile) {
        boolean likelyFloatingBiome = profile.biomeHolder().is(SaturnRingBiomes.TRANSITION_WALL_RING)
                || profile.biomeHolder().is(SaturnRingBiomes.INNER_SPARSE_RING)
                || profile.biomeHolder().is(SaturnRingBiomes.OUTER_SPARSE_RING)
                || profile.biomeHolder().is(SaturnRingBiomes.INNER_FADING_RING);
        if (!likelyFloatingBiome || chance(worldX, RING_Y_CENTER, worldZ, 0x5101L) > 0.018) {
            return;
        }

        int minY = profile.biomeHolder().is(SaturnRingBiomes.TRANSITION_WALL_RING) ? profile.bottomY() + 3 : profile.topY() + 4;
        int maxY = profile.biomeHolder().is(SaturnRingBiomes.TRANSITION_WALL_RING) ? profile.topY() - 3 : profile.topY() + 14;
        if (maxY <= minY) {
            return;
        }

        int centerY = minY + (int) Math.floor(chance(worldX, RING_Y_CENTER, worldZ, 0xBEEFL) * (maxY - minY + 1));
        int radius = chance(worldX, centerY, worldZ, 0xA53DL) < 0.25 ? 2 : 1;
        BlockState state = SaturnRingBlocks.getCoreState(profile.biomeHolder(), sampleMaterialNoise(worldX, centerY, worldZ));

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius + 1) {
                        continue;
                    }
                    if (chance(worldX + dx, centerY + dy, worldZ + dz, 0xD00DL) < 0.20) {
                        continue;
                    }
                    setIfAir(chunk, pos, localX + dx, centerY + dy, localZ + dz, state);
                }
            }
        }
    }

    private void setIfAir(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int localX, int y, int localZ, BlockState state) {
        if (localX < 0 || localX >= 16 || localZ < 0 || localZ >= 16 || y < chunk.getMinBuildHeight() || y >= chunk.getMaxBuildHeight()) {
            return;
        }
        pos.set(localX, y, localZ);
        if (chunk.getBlockState(pos).isAir()) {
            chunk.setBlockState(pos, state, false);
        }
    }

    private static int signedOffset(int x, int z, long salt) {
        return chance(x, 0, z, salt) < 0.5 ? -1 : 1;
    }

    private static double chance(int x, int y, int z, long salt) {
        return (double) (mix(hash(x, y, z, salt)) >>> 11) * 0x1.0p-53;
    }

    private static long hash(int x, int y, int z, long salt) {
        long h = 0x9E3779B97F4A7C15L ^ salt;
        h ^= mix(x * 0x632BE5ABL);
        h ^= mix(y * 0x85157AF5L);
        h ^= mix(z * 0x94D049BBL);
        return mix(h);
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor heightAccessor, RandomState randomState) {
        SaturnRingTerrainProfile profile = sampleProfile(x, z, randomState);
        if (!profile.hasTerrain()) {
            return getMinY();
        }

        int minY = Math.max(profile.bottomY(), heightAccessor.getMinBuildHeight());
        int maxY = Math.min(profile.topY(), heightAccessor.getMaxBuildHeight() - 1);
        for (int y = maxY; y >= minY; y--) {
            if (isSolidTerrain(x, y, z, profile)) {
                return y;
            }
        }
        return getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        SaturnRingTerrainProfile profile = sampleProfile(x, z, randomState);

        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int actualY = heightAccessor.getMinBuildHeight() + i;
            if (isSolidTerrain(x, actualY, z, profile)) {
                states[i] = SaturnRingBlocks.getCoreState(profile.biomeHolder(), sampleMaterialNoise(x, actualY, z));
            } else {
                states[i] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(heightAccessor.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {
    }
}
