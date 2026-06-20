package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SBSDBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SenDims.MOD_ID);

    public static final RegistryObject<Block> SATURN_STONE = register("saturn_stone", Blocks.STONE);
    public static final RegistryObject<Block> SATURN_COBBLESTONE = register("saturn_cobblestone", Blocks.COBBLESTONE);
    public static final RegistryObject<Block> SATURN_DEEPSLATE = register("saturn_deepslate", Blocks.DEEPSLATE);
    public static final RegistryObject<Block> SATURN_COBBLED_DEEPSLATE = register("saturn_cobbled_deepslate", Blocks.COBBLED_DEEPSLATE);
    public static final RegistryObject<Block> SATURN_SANDSTONE = register("saturn_sandstone", Blocks.SANDSTONE);
    public static final RegistryObject<Block> POROUS_SATURN_STONE = register("porous_saturn_stone", Blocks.TUFF);

    private static RegistryObject<Block> register(String name, Block baseBlock) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.copy(baseBlock)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
