package com.tonywww.slashblade_sendims.client;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.sa.HolySwordArts;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.compat.playerAnim.PlayerAnimationOverrider;
import mods.flammpfeil.slashblade.compat.playerAnim.VmdAnimation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Keep the player's arms in sync with the blade's horizontal/vertical cuts. */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HolySwordArtClient {
    private HolySwordArtClient() {}

    @SubscribeEvent
    public static void animations(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("playeranimator")) return;
        event.enqueueWork(() -> {
            ResourceLocation motion = new ResourceLocation(SlashBlade.MODID, "model/pa/player_motion.vmd");
            var animations = PlayerAnimationOverrider.getInstance().getAnimation();
            animations.put(HolySwordArts.CALIBURN, new VmdAnimation(motion, 400, 409, false));
            animations.put(HolySwordArts.CALIBURN_FINISH, new VmdAnimation(motion, 1600, 1621, false));
            animations.put(HolySwordArts.EXCALIBUR, new VmdAnimation(motion, 1600, 1618, false));
        });
    }
}
