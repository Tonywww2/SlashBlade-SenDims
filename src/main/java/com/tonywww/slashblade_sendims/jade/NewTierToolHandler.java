package com.tonywww.slashblade_sendims.jade;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeTier;
import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.addon.harvest.ToolHandler;

import java.util.List;

public class NewTierToolHandler implements ToolHandler {
    public ItemStack displayItem;
    public String name;
    public TagKey<Block> tag;

    public static void registerTierHandler(ForgeTier tier, String name) {
        HarvestToolProvider.registerHandler(new NewTierToolHandler(tier, name));
    }

    public NewTierToolHandler(ForgeTier tier, String name) {
        displayItem = tier.getRepairIngredient().getItems()[0];
        this.name = name;
    }

    @Override
    public ItemStack test(BlockState blockState, Level level, BlockPos blockPos) {
        if (blockState.is(tag)) {
            return displayItem;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public List<ItemStack> getTools() {
        return List.of(displayItem);
    }

    @Override
    public String getName() {
        return name;
    }
}
