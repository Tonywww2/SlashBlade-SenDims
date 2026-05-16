package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.se.*;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SBSDSpecialEffects {
    public static final DeferredRegister<SpecialEffect> SE = DeferredRegister.create(SpecialEffect.REGISTRY_KEY, SenDims.MOD_ID);

    public static final RegistryObject<SpecialEffect> FRENZIED_FLAME = SE.register("frenzied_flame", FrenziedFlame::new);
    public static final RegistryObject<SpecialEffect> ARCANE_A = SE.register("arcane_a", ArcaneA::new);
    public static final RegistryObject<SpecialEffect> THREE_FINGERS = SE.register("three_fingers", ThreeFingers::new);
    public static final RegistryObject<SpecialEffect> DISTANT_THUNDER = SE.register("distant_thunder", DistantThunder::new);
    public static final RegistryObject<SpecialEffect> MANA_DETONATION = SE.register("mana_detonation", ManaDetonation::new);
    public static final RegistryObject<SpecialEffect> INVINCIBLE_PIERCE = SE.register("invincible_pierce", InvinciblePierce::new);
    public static final RegistryObject<SpecialEffect> AFTERSHOCK = SE.register("aftershock", Aftershock::new);
    public static final RegistryObject<SpecialEffect> ARMOR_MELT = SE.register("armor_melt", ArmorMelt::new);
    public static final RegistryObject<SpecialEffect> BLESSING_AND_BANE = SE.register("blessing_and_bane", BlessingAndBane::new);
    public static final RegistryObject<SpecialEffect> MAHAKALA = SE.register("mahakala", Mahakala::new);

    public static void register(IEventBus eventBus) {
        SE.register(eventBus);

    }

}
