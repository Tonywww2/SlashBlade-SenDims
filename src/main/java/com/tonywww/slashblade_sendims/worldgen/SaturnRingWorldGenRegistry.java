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

    // 注册土星环区块生成器
    public static final RegistryObject<Codec<SaturnRingChunkGenerator>> SATURN_RING_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("saturn_ring_chunk_generator", () -> SaturnRingChunkGenerator.CODEC);

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register(eventBus);
        BIOME_SOURCES.register(eventBus);
    }
}
