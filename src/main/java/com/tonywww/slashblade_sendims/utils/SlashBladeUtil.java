package com.tonywww.slashblade_sendims.utils;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;

public class SlashBladeUtil {

    public static final Capability<ISlashBladeState> state = ItemSlashBlade.BLADESTATE;

    public SlashBladeUtil() {
    }

    public static ISlashBladeState getState(ItemStack stack) {
        return stack.getCapability(state).resolve().orElseThrow(() -> new IllegalStateException("ItemStack 缺失 ISlashBladeState 能力"));
    }

}
