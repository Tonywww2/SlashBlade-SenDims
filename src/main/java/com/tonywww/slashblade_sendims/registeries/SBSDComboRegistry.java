package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.sa.FrenziedBurst;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.ability.StunManager;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.util.AttackManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SBSDComboRegistry {
    public static final DeferredRegister<ComboState> COMBO_STATES = DeferredRegister.create(ComboState.REGISTRY_KEY, SenDims.MOD_ID);

    public static final RegistryObject<ComboState> ALL_REUSE = COMBO_STATES.register(
            "all_reuse",
            ComboState.Builder.newInstance().startAndEnd(459, 488).priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation).next(entity -> SlashBlade.prefix("none"))
                    .nextOfTimeout(entity -> SlashBlade.prefix("none"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder().put(0, AttackManager::playQuickSheathSoundAction).build())
                    .releaseAction(ComboState::releaseActionQuickCharge)::build
    );

    public static final RegistryObject<ComboState> FRENZIED_BURST = COMBO_STATES.register("frenzied_burst",
            ComboState.Builder.newInstance().startAndEnd(400, 459).priority(50)
                    .motionLoc(DefaultResources.ExMotionLocation)
                    .next(ComboState.TimeoutNext.buildFromFrame(15, entity -> SlashBlade.prefix("none")))
                    .nextOfTimeout(entity -> SenDims.prefix("all_reuse"))
                    .addTickAction(ComboState.TimeLineTickAction.getBuilder()
                            .put(2, FrenziedBurst::doPreFrenziedBurst)
                            .put(4, FrenziedBurst::doPreFrenziedBurst)
                            .put(6, FrenziedBurst::doPreFrenziedBurst)
                            .put(8, FrenziedBurst::doPreFrenziedBurst)
                            .put(12, FrenziedBurst::doFrenziedBurst)
                            .build())
                    .addHitEffect(StunManager::setStun)
                    ::build);

    public static void register(IEventBus bus) {
        COMBO_STATES.register(bus);

    }
}
