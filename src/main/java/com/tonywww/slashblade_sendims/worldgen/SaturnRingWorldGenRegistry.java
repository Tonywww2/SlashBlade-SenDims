package com.tonywww.slashblade_sendims.worldgen;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/**
 * 将我们的自定义维度生成器注册到 Forge / 原版引擎中。
 * 必须将这些 Codec 注册到相应的注册表中，游戏在读取 Datapack 和生成世界时才能识别反序列化。
 */
public class SaturnRingWorldGenRegistry {

    // 使用 Registry 延迟注册器注册 ChunkGenerator 编解码器
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, SenDims.MOD_ID);

    // 使用 Registry 延迟注册器注册 BiomeSource 编解码器
    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, SenDims.MOD_ID);

    // 注册土星环群系源
    public static final RegistryObject<Codec<SaturnRingBiomeSource>> SATURN_RING_BIOME_SOURCE =
            BIOME_SOURCES.register("saturn_ring_biome_source", () -> SaturnRingBiomeSource.CODEC);

    public static final RegistryObject<Codec<RemappedEndBiomeSource>> REMAPPED_END_BIOME_SOURCE =
            BIOME_SOURCES.register("remapped_end", () -> RemappedEndBiomeSource.CODEC);

    // 注册土星环区块生成器
    public static final RegistryObject<Codec<SaturnRingChunkGenerator>> SATURN_RING_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("saturn_ring_chunk_generator", () -> SaturnRingChunkGenerator.CODEC);

    public static final RegistryObject<Codec<WorldTreeSeaBiomeSource>> WORLD_TREE_SEA_BIOMES =
            BIOME_SOURCES.register("world_tree_sea", () -> WorldTreeSeaBiomeSource.CODEC);

    public static final RegistryObject<Codec<WorldTreeSeaChunkGenerator>> WORLD_TREE_SEA_GENERATOR =
            CHUNK_GENERATORS.register("world_tree_sea", () -> WorldTreeSeaChunkGenerator.CODEC);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, SenDims.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, SenDims.MOD_ID);
    public static final RegistryObject<StructureType<WorldTreeSmallStructure>> WORLD_TREE_SMALL =
            STRUCTURE_TYPES.register("world_tree_small", () -> () -> WorldTreeSmallStructure.CODEC);
    public static final RegistryObject<StructurePieceType> WORLD_TREE_SMALL_PIECE =
            STRUCTURE_PIECES.register("world_tree_small", () -> WorldTreeSmallStructure.Piece::new);

    public static final RegistryObject<StructureType<WorldTreeDungeonStructure>> WORLD_TREE_DUNGEON =
            STRUCTURE_TYPES.register("world_tree_dungeon", () -> () -> WorldTreeDungeonStructure.CODEC);
    public static final RegistryObject<StructurePieceType> WORLD_TREE_DUNGEON_PIECE =
            STRUCTURE_PIECES.register("world_tree_dungeon", () -> WorldTreeDungeonStructure.Piece::new);

    public static final RegistryObject<Codec<AsteroidBeltChunkGenerator>> ASTEROID_BELT_GENERATOR =
            CHUNK_GENERATORS.register("asteroid_belt", () -> AsteroidBeltChunkGenerator.CODEC);
    public static final RegistryObject<StructureType<AsteroidBaseStructure>> ASTEROID_BASE =
            STRUCTURE_TYPES.register("asteroid_base", () -> () -> AsteroidBaseStructure.CODEC);
    public static final RegistryObject<StructurePieceType> ASTEROID_BASE_PIECE =
            STRUCTURE_PIECES.register("asteroid_base", () -> AsteroidBaseStructure.Piece::new);

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        STRUCTURE_PIECES.register(eventBus);
        CHUNK_GENERATORS.register(eventBus);
        BIOME_SOURCES.register(eventBus);
    }
}
