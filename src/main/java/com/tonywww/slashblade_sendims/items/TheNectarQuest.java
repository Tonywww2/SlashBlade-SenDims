package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.curios.TheNectarQuestCapProvider;
import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TheNectarQuest extends AbstractCollectionItem {
    public static final String PATH = "the_nectar_quest";
    public static final String SLOT = "the_nectar_quest";

    public static final int TOTAL_ESS = 7;

    public TheNectarQuest(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new TheNectarQuestCapProvider(stack);
    }

    @Override
    protected String getCollectionPath() {
        return PATH;
    }

    @Override
    protected boolean shouldConsumeFound() {
        return false;
    }

    @Override
    protected @Nullable String getTargetKey(ItemStack targetStack) {
        if (!targetStack.is(SBSDTags.Items.THE_NECTAR_QUEST_ITEMS)) {
            return null;
        }
        return ForgeRegistries.ITEMS.getKey(targetStack.getItem()).toString();
    }

    @Override
    protected long getTotalCollectionCount() {
        return TOTAL_ESS;
    }

    @Override
    protected String getTranslationKeyBase() {
        return "ui.slashblade_sendims.the_nectar_quest";
    }

    @Override
    protected void addSpecificHoverDetails(List<Component> toolTips, ListTag itemList) {
        for (int i = 0; i < itemList.size(); i++) {
            ResourceLocation rl = ResourceLocation.tryParse(itemList.getString(i));
            if (rl != null) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null) {
                    toolTips.add(Component.literal("- ").append(item.getDefaultInstance().getHoverName()));
                }
            }
        }
    }

    public static CompoundTag getTNQTag(ItemStack stack) {
        return getCollectionTag(stack, PATH);
    }
}
