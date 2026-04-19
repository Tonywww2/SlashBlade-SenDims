package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.curios.PrincipleOfSwordArtsCapProvider;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PrincipleOfSwordArts extends Item {
    public static final String PATH = "principle_of_sword_arts";
    public static final String ITEM_LIST = "item_list";
    public static final String ITEM_COUNTS = "item_counts";
    public static final String SLOT = "principle_of_sword_arts";

    public static final int TOTAL_SA = 70;

    public PrincipleOfSwordArts(Properties properties) {
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

        // Check if orb has an SA
        if (!other.is(SlashBladeItems.PROUDSOUL_SPHERE.get()) || !other.hasTag() || !other.getTag().contains("SpecialAttackType")) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }

        String saKey = other.getTag().getString("SpecialAttackType");
        if (saKey.isEmpty() || saKey.equals("slashblade:none")) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }

        CompoundTag tag = getPSATag(self);
        ListTag itemList = tag.getList(ITEM_LIST, Tag.TAG_STRING);

        boolean alreadyContains = false;
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.getString(i).equals(saKey)) {
                alreadyContains = true;
                break;
            }
        }
        if (alreadyContains) {
            player.playSound(SoundEvents.VILLAGER_NO);
            return true;
        }

        itemList.add(StringTag.valueOf(saKey));
        tag.put(ITEM_LIST, itemList);
        tag.putInt(ITEM_COUNTS, tag.getInt(ITEM_COUNTS) + 1);
        other.shrink(1);
        player.playSound(SoundEvents.PLAYER_LEVELUP);
        slot.setChanged();
        return true;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new PrincipleOfSwordArtsCapProvider(stack);
    }

    public static CompoundTag getPSATag(ItemStack stack) {
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
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> toolTips, @NotNull TooltipFlag isAdvanced) {
        CompoundTag tag = getPSATag(stack);
        int counts = tag.getInt(ITEM_COUNTS);

        if (counts > 0) {
            // Note: subtracting 1 because "slashblade:none" shouldn't count.
            if (counts >= TOTAL_SA) {
                toolTips.add(Component.translatable("ui.slashblade_sendims.principle_of_sword_arts.all_collected")
                        .withStyle(ChatFormatting.GOLD));
            }

            toolTips.add(Component.translatable("ui.slashblade_sendims.principle_of_sword_arts.counts", counts));

            if (Screen.hasShiftDown()) {
                toolTips.add(Component.translatable("ui.slashblade_sendims.principle_of_sword_arts.items"));
                ListTag itemList = tag.getList(ITEM_LIST, Tag.TAG_STRING);
                for (int i = 0; i < itemList.size(); i++) {
                    String saKey = itemList.getString(i);
                    ResourceLocation rl = ResourceLocation.tryParse(saKey);
                    if (rl != null && SlashArtsRegistry.REGISTRY.get().containsKey(rl)) {
                        SlashArts sa = SlashArtsRegistry.REGISTRY.get().getValue(rl);
                        if (sa != null) {
                            toolTips.add(Component.literal("- ")
                                    .append(Component.translatable("slashblade.tooltip.slash_art", sa.getDescription())).withStyle(ChatFormatting.GRAY));
                        }
                    }
                }
            } else {
                toolTips.add(Component.translatable("ui.slashblade_sendims.principle_of_sword_arts.shift_for_details"));
            }
        }

        super.appendHoverText(stack, level, toolTips, isAdvanced);
    }
}
