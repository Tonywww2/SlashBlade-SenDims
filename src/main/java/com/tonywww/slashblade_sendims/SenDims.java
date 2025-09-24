package com.tonywww.slashblade_sendims;

import com.mojang.logging.LogUtils;
import com.tonywww.slashblade_sendims.registeries.SBSDCreativeTabs;
import com.tonywww.slashblade_sendims.registeries.SBSDItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SenDims.MOD_ID)
public class SenDims {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "slashblade_sendims";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public SenDims(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        SBSDItems.register(modEventBus);
        SBSDCreativeTabs.register(modEventBus);

    }

    public static ResourceLocation prefix(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
