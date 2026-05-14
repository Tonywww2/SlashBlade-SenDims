package com.tonywww.slashblade_sendims.items;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractCollectionItem extends Item {
    public static final String ITEM_LIST = "item_list";
    public static final String ITEM_COUNTS = "item_counts";

    public AbstractCollectionItem(Properties properties) {
        super(properties);
    }

    protected abstract String getCollectionPath();
    protected abstract boolean shouldConsumeFound();
    protected abstract @Nullable String getTargetKey(ItemStack targetStack);
    protected abstract long getTotalCollectionCount();
    protected abstract String getTranslationKeyBase();
    protected abstract void addSpecificHoverDetails(List<Component> toolTips, ListTag itemList);

    public static CompoundTag getCollectionTag(ItemStack stack, String path) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(path)) {
            CompoundTag data = new CompoundTag();
            data.put(ITEM_LIST, new ListTag());
            data.putInt(ITEM_COUNTS, 0);
            tag.put(path, data);
        }
        return tag.getCompound(path);
    }

    public CompoundTag getCollectionTag(ItemStack stack) {
        return getCollectionTag(stack, getCollectionPath());
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack self, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess access) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return false;
        }
        if (other.isEmpty()) {
            return false;
        }

        player.getCooldowns().addCooldown(this, 20);

        String targetKey = getTargetKey(other);
        if (targetKey == null) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }

        CompoundTag tag = getCollectionTag(self);
        ListTag itemList = tag.getList(ITEM_LIST, Tag.TAG_STRING);

        boolean alreadyContains = false;
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.getString(i).equals(targetKey)) {
                alreadyContains = true;
                break;
            }
        }

        if (alreadyContains) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }

        itemList.add(StringTag.valueOf(targetKey));
        tag.put(ITEM_LIST, itemList);
        tag.putInt(ITEM_COUNTS, tag.getInt(ITEM_COUNTS) + 1);

        if (shouldConsumeFound()) {
            other.shrink(1);
        }
        player.playSound(SoundEvents.PLAYER_LEVELUP);
        slot.setChanged();
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> toolTips, @NotNull TooltipFlag isAdvanced) {
        CompoundTag tag = getCollectionTag(stack);
        int counts = tag.getInt(ITEM_COUNTS);

        if (counts > 0) {
            long total = getTotalCollectionCount();
            String transBase = getTranslationKeyBase();

            if (total > 0 && counts >= total) {
                toolTips.add(Component.translatable(transBase + ".all_collected").withStyle(ChatFormatting.GOLD));
            }

            toolTips.add(Component.translatable(transBase + ".counts", counts));

            if (Screen.hasShiftDown()) {
                toolTips.add(Component.translatable(transBase + ".items"));
                ListTag itemList = tag.getList(ITEM_LIST, Tag.TAG_STRING);
                addSpecificHoverDetails(toolTips, itemList);
            } else {
                toolTips.add(Component.translatable(transBase + ".shift_for_details"));
            }
        }
        super.appendHoverText(stack, level, toolTips, isAdvanced);
    }
}

