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

public class BloodJade extends Item {

    public static final String KILL_COUNT_PATH = "sbsd.bj.kill_count";

    public BloodJade(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(@NotNull ItemStack me, @NotNull ItemStack other, @NotNull Slot slot, @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess slotAccess) {
        if (action != ClickAction.SECONDARY) return false;

        if (!(other.getItem() instanceof ItemSlashBlade)) return false;
        CompoundTag data = me.getOrCreateTag();
        if (!data.contains(KILL_COUNT_PATH)) return false;
        int countToIncrease = data.getInt(KILL_COUNT_PATH);
        ISlashBladeState bladeState = SlashBladeUtil.getState(other);
        if (bladeState == null) return false;
        int newCount = bladeState.getKillCount() + countToIncrease;
        bladeState.setKillCount(newCount);
        if (other.getTag() == null) return false;
        other.getTag().getCompound("bladeState").putInt("killCount", newCount);

        me.shrink(1);
        player.playSound(SoundEvents.PLAYER_LEVELUP);
        slot.setChanged();

        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level pLevel, @NotNull List<Component> tooltips, @NotNull TooltipFlag pIsAdvanced) {
        var tag = stack.getTag();
        if (tag != null) {
            if (tag.contains(KILL_COUNT_PATH)) {
                tooltips.add(Component.translatable("slashblade.tooltip.killcount", tag.getInt(KILL_COUNT_PATH)).withStyle(ChatFormatting.RED));

            }
        }
    }

    public static ItemStack withKillCount(int count) {
        ItemStack stack = new ItemStack(SBSDItems.BLOOD_JADE.get());
        stack.getOrCreateTag().putInt(KILL_COUNT_PATH, count);
        return stack;
    }

}
