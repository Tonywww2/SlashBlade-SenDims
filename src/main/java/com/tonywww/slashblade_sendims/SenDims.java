package com.tonywww.slashblade_sendims;

import com.mojang.logging.LogUtils;
import com.tonywww.slashblade_sendims.config.SenDimsConfig;
import com.tonywww.slashblade_sendims.network.HolySwordVisualPacket;
import net.minecraftforge.network.NetworkDirection;
import java.util.Optional;
import com.tonywww.slashblade_sendims.kubejs.SBSDPlugin;
import com.tonywww.slashblade_sendims.network.LeaderStateSyncPacket;
import com.tonywww.slashblade_sendims.network.MadnessSyncPacket;
import com.tonywww.slashblade_sendims.registeries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import com.tonywww.slashblade_sendims.worldgen.SaturnRingWorldGenRegistry;
import se.mickelus.mutil.scheduling.ServerScheduler;

@Mod(SenDims.MOD_ID)
public class SenDims {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "slashblade_sendims";

    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ServerScheduler serverScheduler;

    private static final String PROTOCOL_VERSION = "7";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            prefix("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public SenDims(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        context.registerConfig(ModConfig.Type.COMMON, SenDimsConfig.SPEC, SenDimsConfig.FILE_NAME);
        MinecraftForge.EVENT_BUS.register(serverScheduler = new ServerScheduler());

        SBSDPlugin.register(modEventBus);

        modEventBus.addListener(this::clientSetup);

        SBSDParticles.registerCommon(modEventBus);
        com.tonywww.slashblade_sendims.yakuman.YakumanRegistry.bootstrap();
        SBSDSpecialEffects.register(modEventBus);
        SBSDMobEffects.register(modEventBus);
        SBSDComboRegistry.register(modEventBus);
        SBSDSlashArtRegistry.register(modEventBus);

        SBSDEntities.register(modEventBus);
        SBSDAttributes.register(modEventBus);
        SBSDBlocks.register(modEventBus);
        SBSDItems.register(modEventBus);
        SBSDCreativeTabs.register(modEventBus);

        SaturnRingWorldGenRegistry.register(modEventBus);

        registerPackets();

    }

    public static void registerPackets() {
        int id = 0;
        NETWORK.registerMessage(id++, MadnessSyncPacket.class,
                MadnessSyncPacket::encode,
                MadnessSyncPacket::decode,
                MadnessSyncPacket::handle);
        NETWORK.registerMessage(id++, LeaderStateSyncPacket.class,
            LeaderStateSyncPacket::encode,
            LeaderStateSyncPacket::decode,
            LeaderStateSyncPacket::handle);
        NETWORK.registerMessage(id++, HolySwordVisualPacket.class,
                HolySwordVisualPacket::encode, HolySwordVisualPacket::decode,
                HolySwordVisualPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NETWORK.registerMessage(id++, com.tonywww.slashblade_sendims.network.SukunaVisualPacket.class,
                com.tonywww.slashblade_sendims.network.SukunaVisualPacket::encode,
                com.tonywww.slashblade_sendims.network.SukunaVisualPacket::decode,
                com.tonywww.slashblade_sendims.network.SukunaVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    static {
        NETWORK.registerMessage(5, com.tonywww.slashblade_sendims.network.GoldenCruxVisualPacket.class,
                com.tonywww.slashblade_sendims.network.GoldenCruxVisualPacket::encode,
                com.tonywww.slashblade_sendims.network.GoldenCruxVisualPacket::decode,
                com.tonywww.slashblade_sendims.network.GoldenCruxVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        NETWORK.registerMessage(4, com.tonywww.slashblade_sendims.yakuman.YakumanVisualPacket.class,
                com.tonywww.slashblade_sendims.yakuman.YakumanVisualPacket::encode,
                com.tonywww.slashblade_sendims.yakuman.YakumanVisualPacket::decode,
                com.tonywww.slashblade_sendims.yakuman.YakumanVisualPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    @SuppressWarnings("removal")
    private void clientSetup(final FMLClientSetupEvent event) {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        event.enqueueWork(() -> {
            SBSDParticles.registerClient(bus);

        });

    }

    public static ResourceLocation prefix(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
