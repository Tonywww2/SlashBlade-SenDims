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
    public static final RegistryObject<SlashArts> UNENDURABLE_FRENZY = register("unendurable_frenzy", SBSDComboRegistry.UNENDURABLE_FRENZY.getId());
    public static final RegistryObject<SlashArts> GOLDEN_CRUX = register("golden_crux", SBSDComboRegistry.GOLDEN_CRUX.getId());
    public static final RegistryObject<SlashArts> GOLDEN_CRUX_EX = register("golden_crux_ex", SBSDComboRegistry.GOLDEN_CRUX_EX.getId());
    public static final RegistryObject<SlashArts> MAHAKALA = register("mahakala", SBSDComboRegistry.MAHAKALA.getId());

    public static final RegistryObject<SlashArts> EXPLOSIVE_DAWN_AMMO = register("explosive_dawn_ammo", SBSDComboRegistry.EXPLOSIVE_DAWN_AMMO.getId());
    public static final RegistryObject<SlashArts> WAVE_EDGE_AMMO = register("wave_edge_ammo", SBSDComboRegistry.WAVE_EDGE_AMMO.getId());
    public static final RegistryObject<SlashArts> VOID_SLASH_AMMO = register("void_slash_ammo", SBSDComboRegistry.VOID_SLASH_AMMO.getId());
    public static final RegistryObject<SlashArts> SAKURA_END_AMMO = register("sakura_end_ammo", SBSDComboRegistry.SAKURA_END_AMMO.getId());

    public static RegistryObject<SlashArts> register(String name, ResourceLocation resourceLocation) {
        return SLASH_ARTS.register(name, () -> new SlashArts((e) -> resourceLocation));
    }

    public static void register(IEventBus bus) {
        SLASH_ARTS.register(bus);
    }

}
