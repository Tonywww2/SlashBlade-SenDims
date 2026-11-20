package com.tonywww.slashblade_sendims.registeries;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.effect.AmplifyEffect;
import com.tonywww.slashblade_sendims.effect.AvalonEffect;
import com.tonywww.slashblade_sendims.effect.DimensionStasisEffect;
import com.tonywww.slashblade_sendims.effect.StormPenetrationEffect;
import com.tonywww.slashblade_sendims.effect.ManaBurstEffect;
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

    /** 风暴穿透：每级 +5% 攻击力（MULTIPLY_TOTAL），由无名王者系列 SE「风暴」赋予 */
    public static final RegistryObject<MobEffect> STORM_PENETRATION =
            MOB_EFFECTS.register("storm_penetration", StormPenetrationEffect::new);

    public static final RegistryObject<MobEffect> MANA_BURST =
            MOB_EFFECTS.register("mana_burst", ManaBurstEffect::new);

    /** 增幅：每级 +5% 攻击力（MULTIPLY_BASE），由莉芙系列 SA 赋予 */
    public static final RegistryObject<MobEffect> AMPLIFY =
            MOB_EFFECTS.register("amplify", AmplifyEffect::new);

    /** 阿瓦隆：誓约胜利之剑 SE 的无敌显示，实际无敌与拔刀剑闪避共用 */
    public static final RegistryObject<MobEffect> AVALON =
            MOB_EFFECTS.register("avalon", AvalonEffect::new);

    /** 次元凝滞：天顶·裂空 SA「真·次元斩」施加，I 级减速 80%（首领和玩家），II 级完全定住 */
    public static final RegistryObject<MobEffect> DIMENSION_STASIS =
            MOB_EFFECTS.register("dimension_stasis", DimensionStasisEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
