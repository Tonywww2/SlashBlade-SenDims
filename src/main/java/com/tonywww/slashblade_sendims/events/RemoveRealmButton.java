package com.tonywww.slashblade_sendims.events;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RemoveRealmButton {
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            event.getScreen().renderables.removeIf(renderable -> {
                if (renderable instanceof Button button)
                    return button.getMessage().equals(Component.translatable("menu.online"));

                return false;
            });
        }
    }
}
