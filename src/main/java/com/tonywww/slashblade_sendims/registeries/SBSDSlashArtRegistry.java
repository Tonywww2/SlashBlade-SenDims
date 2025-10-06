package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SBSDSlashArtRegistry {

    public static final DeferredRegister<SlashArts> SLASH_ARTS = DeferredRegister.create(SlashArts.REGISTRY_KEY, SenDims.MOD_ID);

    public static final RegistryObject<SlashArts> FRENZIED_BURST = register("frenzied_burst", SBSDComboRegistry.FRENZIED_BURST.getId());

    public static RegistryObject<SlashArts> register(String name, ResourceLocation resourceLocation) {
        return SLASH_ARTS.register(name, () -> new SlashArts((e) -> resourceLocation));
    }

    public static void register(IEventBus bus) {
        SLASH_ARTS.register(bus);
    }

}
