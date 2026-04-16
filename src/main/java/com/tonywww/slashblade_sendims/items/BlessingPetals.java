package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.curios.BlessingPetalsCapProvider;
import com.tonywww.slashblade_sendims.registeries.SBSDTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlessingPetals extends Item {
    public static final String PATH = "blessing_petals";
    public static final String ITEM_LIST = "item_list";
    public static final String ITEM_COUNTS = "item_counts";
    public static final String SLOT = "blessing_petals";

    public BlessingPetals(Properties properties) {
        super(properties);
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
        if (!other.is(SBSDTags.Items.BLESSING_PETALS_ITEMS)) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }
        CompoundTag tag = getBPTag(self);
        ListTag itemList = tag.getList(ITEM_LIST, Tag.TAG_STRING);
        String itemId = ForgeRegistries.ITEMS.getKey(other.getItem()).toString();

        boolean alreadyContains = false;
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.getString(i).equals(itemId)) {
                alreadyContains = true;
                break;
            }
        }
        if (alreadyContains) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }

        itemList.add(StringTag.valueOf(itemId));
        tag.put(ITEM_LIST, itemList);
        tag.putInt(ITEM_COUNTS, tag.getInt(ITEM_COUNTS) + 1);
        other.shrink(1);
        player.playSound(SoundEvents.PLAYER_LEVELUP);
        slot.setChanged();
        return true;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new BlessingPetalsCapProvider(stack);
    }

    public static CompoundTag getBPTag(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(PATH)) {
            CompoundTag data = new CompoundTag();
            data.put(ITEM_LIST, new ListTag());
            data.putInt(ITEM_COUNTS, 0);
            tag.put(PATH, data);
        }
        return tag.getCompound(PATH);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> toolTips, @NotNull TooltipFlag isAdvanced) {
        CompoundTag tag = getBPTag(stack);
        int counts = tag.getInt(ITEM_COUNTS);

        toolTips.add(Component.translatable("ui.slashblade_sendims.blessing_petals.counts", counts));

        if (counts > 0) {
            if (Screen.hasShiftDown()) {
                toolTips.add(Component.translatable("ui.slashblade_sendims.blessing_petals.items"));
                ListTag itemList = tag.getList(ITEM_LIST, Tag.TAG_STRING);
                for (int i = 0; i < itemList.size(); i++) {
                    String itemId = itemList.getString(i);
                    ResourceLocation rl = ResourceLocation.tryParse(itemId);
                    if (rl != null) {
                        Item item = ForgeRegistries.ITEMS.getValue(rl);
                        if (item instanceof RecordItem recordItem)
                            toolTips.add(Component.literal("- ").append(recordItem.getDisplayName()));

                    }
                }
            } else {
                toolTips.add(Component.translatable("ui.slashblade_sendims.blessing_petals.shift_for_details"));
            }
        }

        super.appendHoverText(stack, level, toolTips, isAdvanced);
    }
}
