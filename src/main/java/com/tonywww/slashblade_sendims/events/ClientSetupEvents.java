package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.client.renderer.entity.ChaoticBlisteringSwordsRenderer;
import com.tonywww.slashblade_sendims.client.renderer.entity.ChaoticJudgementCutRenderer;
import com.tonywww.slashblade_sendims.client.overlay.MadnessOverlay;
import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import mods.flammpfeil.slashblade.client.renderer.entity.SlashEffectRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetupEvents {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                SBSDEntities.CHAOTIC_JUDGEMENT_CUT.get(),
                ChaoticJudgementCutRenderer::new
        );
        event.registerEntityRenderer(
                SBSDEntities.CHAOTIC_SLASH_EFFECT.get(),
                SlashEffectRenderer::new
        );
        event.registerEntityRenderer(
                SBSDEntities.CHAOTIC_BLISTERING_SWORDS.get(),
                ChaoticBlisteringSwordsRenderer::new
        );
    }

    @SubscribeEvent
    public static void registerGuiOverlay(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("slashblade_sendims.madness", MadnessOverlay.INSTANCE);
        event.registerBelowAll("slashblade_sendims.ammo", com.tonywww.slashblade_sendims.client.overlay.AmmoSAOverlay.INSTANCE);

    }
}
