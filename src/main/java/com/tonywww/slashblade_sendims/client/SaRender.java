package com.tonywww.slashblade_sendims.client;

import com.tonywww.slashblade_sendims.SenDims;
import mods.flammpfeil.slashblade.registry.SlashArtsRegistry;
import mods.flammpfeil.slashblade.registry.SlashBladeItems;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SaRender {
    public static boolean saDecorator(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
        if(stack.getTag() != null) {
            var tag = stack.getTag();
            if(tag.contains("SpecialAttackType")){
                ResourceLocation SA =  ResourceLocation.parse(tag.getString("SpecialAttackType"));
                if (( SlashArtsRegistry.REGISTRY.get()).containsKey(SA) && !Objects.equals((SlashArtsRegistry.REGISTRY.get()).getValue(SA), SlashArtsRegistry.NONE.get())) {
                    var pose = guiGraphics.pose();
                    pose.pushPose();
                    pose.translate(xOffset+1,yOffset+11,256);
                    pose.scale(0.4f,0.4f,1);
                    guiGraphics.drawString(
                            font,
                            font.getSplitter().formattedHeadByWidth(SlashArtsRegistry.REGISTRY.get().getValue(SA).getDescription().getString(),9*4, Style.EMPTY),
                            0,0,
                            -1,false
                            );
                    pose.popPose();

                }
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(SlashBladeItems.PROUDSOUL_SPHERE.get(), SaRender::saDecorator);
    }
}
