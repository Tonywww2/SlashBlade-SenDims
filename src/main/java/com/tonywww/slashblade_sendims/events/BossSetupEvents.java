package com.tonywww.slashblade_sendims.events;

import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.api.leader.LeaderApi;
import com.tonywww.slashblade_sendims.api.leader.LeaderProfile;
import com.tonywww.slashblade_sendims.entities.boss.TwilightBladeWarden;
import com.tonywww.slashblade_sendims.registeries.SBSDEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BossSetupEvents {
    private BossSetupEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SBSDEntities.TWILIGHT_BLADE_WARDEN.get(), TwilightBladeWarden.createAttributes().build());
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (!LeaderApi.registerLeaderType(SBSDEntities.TWILIGHT_BLADE_WARDEN.get(), LeaderProfile.EXTERNAL)) {
                SenDims.LOGGER.warn("Twilight Blade Warden already has a different Leader profile");
            }
        });
    }
}
