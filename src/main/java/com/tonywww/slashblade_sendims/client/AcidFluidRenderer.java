package com.tonywww.slashblade_sendims.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tonywww.slashblade_sendims.SenDims;
import igentuman.nc.setup.registration.NCFluids;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AcidFluidRenderer {

    private static final ResourceLocation ACID_OVERLAY = SenDims.prefix("textures/misc/acid_underwater.png");
    private static final String SULFURIC_ACID_PATH = "sulfuric_acid";
    private static Fluid SULFURIC_ACID_STILL;
    private static Fluid SULFURIC_ACID_FLOWING;

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        SULFURIC_ACID_STILL = NCFluids.ALL_FLUID_ENTRIES.get(SULFURIC_ACID_PATH).getStill();
        SULFURIC_ACID_FLOWING = NCFluids.ALL_FLUID_ENTRIES.get(SULFURIC_ACID_PATH).getFlowing();
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Camera camera = event.getCamera();
        FluidState fluidState = camera.getEntity().level().getFluidState(camera.getBlockPosition());

        if (fluidState.is(SULFURIC_ACID_STILL) || fluidState.is(SULFURIC_ACID_FLOWING)) {
            event.setRed(0.7F);
            event.setGreen(0.7F);
            event.setBlue(0.4F);
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Camera camera = event.getCamera();
        FluidState fluidState = camera.getEntity().level().getFluidState(camera.getBlockPosition());

        if (fluidState.is(SULFURIC_ACID_STILL) || fluidState.is(SULFURIC_ACID_FLOWING)) {
            event.setNearPlaneDistance(-8f);
            event.setFarPlaneDistance(160f);
            event.setCanceled(true);

        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        FluidState fluidState = camera.getEntity().level().getFluidState(camera.getBlockPosition());

        if (fluidState.is(SULFURIC_ACID_STILL) || fluidState.is(SULFURIC_ACID_FLOWING)) {
            renderAcidOverlay(event.getGuiGraphics().pose());
        }
    }

    private static void renderAcidOverlay(PoseStack poseStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, ACID_OVERLAY);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.35F);

        Minecraft minecraft = Minecraft.getInstance();
        float width = minecraft.getWindow().getGuiScaledWidth();
        float height = minecraft.getWindow().getGuiScaledHeight();

        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, 0.0F, height, -90.0F).uv(0.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix, width, height, -90.0F).uv(1.0F, 1.0F).endVertex();
        bufferBuilder.vertex(matrix, width, 0.0F, -90.0F).uv(1.0F, 0.0F).endVertex();
        bufferBuilder.vertex(matrix, 0.0F, 0.0F, -90.0F).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}