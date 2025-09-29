package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.items.StructureQuill;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SBSDCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SenDims.MOD_ID);

    public static RegistryObject<CreativeModeTab> SBS_TAB = CREATIVE_MODE_TABS.register("palmon_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(SBSDItems.DEEPREALM_CERTIFICATE.get()))
                    .title(Component.translatable("creativetab.slashblade_sendims_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(SBSDItems.DEEPREALM_CERTIFICATE.get());
                        pOutput.accept(SBSDItems.STRUCTURE_QUILL.get());
                        pOutput.accept(StructureQuill.forStructure("minecraft:stronghold"));

                    })
                    .build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
