package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.se.FrenziedFlame;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SBSDSpecialEffects {
    public static final DeferredRegister<SpecialEffect> SE =DeferredRegister.create(SpecialEffect.REGISTRY_KEY, SenDims.MOD_ID);

    public static final RegistryObject<SpecialEffect> FRENZIED_FLAME = SE.register("frenzied_flame", FrenziedFlame::new);


    public static void register(IEventBus eventBus) {
        SE.register(eventBus);

    }

}
