package com.tonywww.slashblade_sendims.yakuman;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.registeries.*;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.init.DefaultResources;
import mods.flammpfeil.slashblade.registry.combo.ComboState;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import java.util.Arrays;

public final class YakumanRegistry {
    public enum Effect {
        WIND_CYCLE, BORROW_WIND, THREE_SEALS, SEVEN_EDICT, HIDDEN_TRIPLETS, TERMINALS,
        JADE_LIFE, KONG, SOLITARY, HEAVEN, NINE_LAMPS, EARTH, FOUR_SEALS, WIND_VEIL;
        public final ResourceLocation id=SenDims.prefix("yakuman_"+name().toLowerCase(java.util.Locale.ROOT));
        public final int bit=1<<ordinal();
    }
    private static boolean initialized;
    public static void bootstrap() {
        if(initialized)return;
        initialized=true;
        for(var spec:YakumanSpec.values()) {
            SBSDComboRegistry.COMBO_STATES.register(spec.id.getPath(),()->ComboState.Builder.newInstance()
                    .startAndEnd(400,429).priority(50).motionLoc(DefaultResources.ExMotionLocation)
                    .next(new ComboState.TimeoutNext(8,e->SlashBlade.prefix("none")))
                    .nextOfTimeout(e->SlashBlade.prefix("none")).clickAction(e->YakumanArts.begin(e,spec)).build());
            SBSDSlashArtRegistry.SLASH_ARTS.register(spec.id.getPath(),()->new SlashArts(e->spec.id)
                    .setComboStateJust(e->spec.id).setComboStateSuper(e->spec.id));
        }
        for(var effect:Effect.values())
            SBSDSpecialEffects.SE.register(effect.id.getPath(),()->new SpecialEffect(0));
    }
    public static int effects(mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState state) {
        if(state==null || state.isBroken() || state.isSealed())return 0;
        int mask=0;
        for(var effect:Effect.values())if(state.hasSpecialEffect(effect.id))mask|=effect.bit;
        return mask;
    }
    public static boolean has(int mask,Effect effect) { return (mask&effect.bit)!=0; }
}
