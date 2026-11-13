package com.tonywww.slashblade_sendims.client.boss;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.client.ClientLeaderIndicatorApi;
import com.tonywww.slashblade_sendims.client.renderer.entity.TwilightBladeWardenRenderer;
import com.tonywww.slashblade_sendims.entities.boss.TwilightBladeWarden;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.OptionalDouble;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WardenClientSetup {
    private WardenClientSetup() {
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(TwilightBladeWardenRenderer.SWORD_MODEL);
    }

    /** 红刀招架窗口的「危」、粒子环与钟声由 Leader 核心绘制，这里只提供前摇进度。只注册一次。 */
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ClientLeaderIndicatorApi.registerExternalWarningProvider(entity ->
                entity instanceof TwilightBladeWarden warden
                        ? warden.redWarningProgress()
                        : OptionalDouble.empty()));
    }
}
