package com.tonywww.slashblade_sendims;

import com.mojang.logging.LogUtils;
import com.tonywww.slashblade_sendims.kubejs.SBSDPlugin;
import com.tonywww.slashblade_sendims.network.MadnessSyncPacket;
import com.tonywww.slashblade_sendims.registeries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import se.mickelus.mutil.scheduling.ServerScheduler;

@Mod(SenDims.MOD_ID)
public class SenDims {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "slashblade_sendims";

    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ServerScheduler serverScheduler;

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            prefix("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );


    public SenDims(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        MinecraftForge.EVENT_BUS.register(serverScheduler = new ServerScheduler());

        SBSDPlugin.register(modEventBus);

        modEventBus.addListener(this::clientSetup);

        SBSDParticles.registerCommon(modEventBus);
        SBSDSpecialEffects.register(modEventBus);
        SBSDComboRegistry.register(modEventBus);
        SBSDSlashArtRegistry.register(modEventBus);

        SBSDAttributes.register(modEventBus);
        SBSDItems.register(modEventBus);
        SBSDCreativeTabs.register(modEventBus);

        registerPackets();

    }

    public static void registerPackets() {
        int id = 0;
        NETWORK.registerMessage(id++, MadnessSyncPacket.class,
                MadnessSyncPacket::encode,
                MadnessSyncPacket::decode,
                MadnessSyncPacket::handle);
    }

    @SuppressWarnings("removal")
    private void clientSetup(final FMLClientSetupEvent event) {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(SBSDParticles::registerClient);

    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
