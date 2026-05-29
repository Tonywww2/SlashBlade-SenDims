package com.tonywww.slashblade_sendims.compat.jei;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.List;

public record StackData(String lang, List<ItemStack> putItem, ItemStack targetItem) {
    public static List<StackData> AllData = new ObjectArrayList<>();

    static {
        if (FMLEnvironment.production)
            register("debug_jei_info", List.of(Items.ANDESITE.getDefaultInstance()), Items.ALLIUM.getDefaultInstance());
    }

    public static void clear() {
        AllData.clear();
    }

    public static void register(StackData data) {
        AllData.add(data);
    }

    public static void register(String lang, List<ItemStack> putItem, ItemStack targetItem) {
        register(new StackData(lang, putItem, targetItem));
    }
}
