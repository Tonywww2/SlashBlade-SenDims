package com.tonywww.slashblade_sendims.curios;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;

public class DeepRealmCertificateCapProvider implements ICapabilityProvider {

    private final ItemStack stack;
    private final DeepRealmCertificateWrapper curiosInstance;

    public DeepRealmCertificateCapProvider(ItemStack stack) {
        this.stack = stack;
        this.curiosInstance = new DeepRealmCertificateWrapper(stack);
    }


    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CuriosCapability.ITEM) {
            return LazyOptional.of(this::getCuriosInstance).cast();
        }
        return LazyOptional.empty();

    }

    public ItemStack getStack() {
        return stack;
    }

    public DeepRealmCertificateWrapper getCuriosInstance() {
        return curiosInstance;
    }
}
