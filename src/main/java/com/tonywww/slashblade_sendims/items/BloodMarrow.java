package com.tonywww.slashblade_sendims.items;

import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import com.tonywww.slashblade_sendims.utils.SlashBladeUtil;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
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

/**
 * 血髓：与血玉用法相同，右键叠到拔刀剑上（或拿着拔刀剑右键它），把记录的耀魂数加到刀上并消耗一个。
 */
public class BloodMarrow extends Item {

    public static final String PROUD_SOUL_PATH = "sbsd.bm.proud_soul";

    public BloodMarrow(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack me, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess slotAccess) {
        if (action != ClickAction.SECONDARY) return false;
        if (!(other.getItem() instanceof ItemSlashBlade)) return false;
        if (!addProudSoul(other, me)) return false;

        me.shrink(1);
        player.playSound(SoundEvents.PLAYER_LEVELUP);
        slot.setChanged();

        return true;
    }

    /**
     * 把血髓记录的耀魂数加到刀上。血髓没有记录、或刀没有状态时返回 false，且不改动任何东西。
     */
    public static boolean addProudSoul(ItemStack blade, ItemStack marrow) {
        CompoundTag data = marrow.getTag();
        if (data == null || !data.contains(PROUD_SOUL_PATH)) return false;
        ISlashBladeState bladeState = SlashBladeUtil.getState(blade);
        if (bladeState == null || blade.getTag() == null) return false;
        long sum = (long) bladeState.getProudSoulCount() + data.getInt(PROUD_SOUL_PATH);
        int newCount = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, sum));
        bladeState.setProudSoulCount(newCount);
        // 与血玉相同：同步写入刀状态 NBT，保证客户端立即刷新
        blade.getTag().getCompound("bladeState").putInt("proudSoul", newCount);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level pLevel, @NotNull List<Component> tooltips, @NotNull TooltipFlag pIsAdvanced) {
        var tag = stack.getTag();
        if (tag != null && tag.contains(PROUD_SOUL_PATH)) {
            tooltips.add(Component.translatable("slashblade.tooltip.proud_soul", tag.getInt(PROUD_SOUL_PATH)).withStyle(ChatFormatting.GOLD));
        }
    }

    public static ItemStack withProudSoul(int count) {
        ItemStack stack = new ItemStack(SBSDItems.BLOOD_MARROW.get());
        stack.getOrCreateTag().putInt(PROUD_SOUL_PATH, count);
        return stack;
    }

}
