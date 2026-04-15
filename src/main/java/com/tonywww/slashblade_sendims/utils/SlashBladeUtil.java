package com.tonywww.slashblade_sendims.utils;

import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;

public class SlashBladeUtil {

    public static final Capability<ISlashBladeState> state = ItemSlashBlade.BLADESTATE;

    public SlashBladeUtil() {
    }

    public static ISlashBladeState getState(ItemStack stack) {
        return stack.getCapability(state).resolve().orElseThrow(() -> new IllegalStateException("ItemStack 缺失 ISlashBladeState 能力"));
    }

    public static ItemStack getBladeItemStack(RegistryAccess registryAccess, ResourceLocation bladeKey) {

        return registryAccess.registryOrThrow(SlashBladeDefinition.REGISTRY_KEY)
                .getOrThrow(ResourceKey.create(SlashBladeDefinition.REGISTRY_KEY, bladeKey))
                .getBlade();
    }

}
