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

    // ---- 无名王者系列 (Nameless King) ----
    /** 无名「褪名」：空闲 5 秒后命中会钳制耐久 */
    public static final RegistryObject<SpecialEffect> NAMELESS_SE_FADED =
            SE.register("nameless_se_faded", NamelessFaded::new);
    /** 无名「孤高」起：斩击命中累积王威充能，满层恢复 AP */
    public static final RegistryObject<SpecialEffect> NAMELESS_SE_SOVEREIGNTY =
            SE.register("nameless_se_sovereignty", NamelessSovereignty::new);
    /** 无名王者「无冠」：王威·极（40 层 / 20% AP） */
    public static final RegistryObject<SpecialEffect> NAMELESS_SE_SOVEREIGNTY_EX =
            SE.register("nameless_se_sovereignty_ex", NamelessSovereignty::new);
    /** 无名「雷霆」起：SA 释放赋予风暴穿透并落雷 */
    public static final RegistryObject<SpecialEffect> NAMELESS_SE_STORM =
            SE.register("nameless_se_storm", NamelessStorm::new);
    /** 无名「天罚」起：对精英/Boss 斩击附加真实伤害 */
    public static final RegistryObject<SpecialEffect> NAMELESS_SE_SUPPRESSION =
            SE.register("nameless_se_suppression", NamelessSuppression::new);
    /** 无名王者「无冠」：加冕标记，替换褪名 */
    public static final RegistryObject<SpecialEffect> NAMELESS_SE_CROWNLESS =
            SE.register("nameless_se_crownless", NamelessCrownless::new);

    public static void register(IEventBus eventBus) {
        SE.register(eventBus);

    }

}
