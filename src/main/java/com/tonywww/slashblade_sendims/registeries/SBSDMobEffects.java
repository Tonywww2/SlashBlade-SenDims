package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.effect.StormPenetrationEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 本模组自定义药水效果注册表。
 */
public class SBSDMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SenDims.MOD_ID);

    /** 风暴穿透：每级 +5 魔法穿透，由无名王者系列 SE「风暴」赋予 */
    public static final RegistryObject<MobEffect> STORM_PENETRATION =
            MOB_EFFECTS.register("storm_penetration", StormPenetrationEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
