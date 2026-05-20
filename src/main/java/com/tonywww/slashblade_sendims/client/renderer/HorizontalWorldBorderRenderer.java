package com.tonywww.slashblade_sendims.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tonywww.slashblade_sendims.SBSDValues;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HorizontalWorldBorderRenderer {

    private static final ResourceLocation FORCEFIELD_LOCATION = ResourceLocation.parse("textures/misc/forcefield.png");

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        ResourceLocation dimId = level.dimension().location();
        if (!SBSDValues.HEIGHT_BOARDER_Y.containsKey(dimId)) return;

        Camera camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        double targetY = SBSDValues.HEIGHT_BOARDER_Y.get(dimId);

        double d0 = (mc.options.getEffectiveRenderDistance() * 16d);
        double distY = Math.abs(camY - targetY);

        if (distY > d0) return;

        double alphaFade = 1.0D - (distY / d0);
        alphaFade = Math.pow(alphaFade, 4.0D);
        alphaFade = Mth.clamp(alphaFade, 0.0D, 1.0D);

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
        RenderSystem.depthMask(Minecraft.useShaderTransparency());

        PoseStack posestack = RenderSystem.getModelViewStack();
        posestack.pushPose();
        RenderSystem.applyModelViewMatrix();

        int color = level.getWorldBorder().getStatus().getColor();
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, (float) alphaFade);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.polygonOffset(-3.0F, -3.0F);
        RenderSystem.enablePolygonOffset();
        RenderSystem.disableCull();

        float timeF = (float) (Util.getMillis() % 3000L) / 3000.0F;

        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        double minX = Math.floor(camX - d0);
        double maxX = Math.ceil(camX + d0);
        double minZ = Math.floor(camZ - d0);
        double maxZ = Math.ceil(camZ + d0);

        double yRender = targetY - camY;

        // Break into smaller quads similar to Vanilla if needed, but since it's perfectly horizontal,
        // one large quad or a few quads might be entirely fine. Vanilla breaks it to avoid precision issues over huge distances.
        // We will loop over 1-block increments similar to vanilla, to ensure UV mapping is consistent.

        // Actually, large quads are completely fine as long as UVs don't exceed float precision.
        double startX = minX - camX;
        double endX = maxX - camX;
        double startZ = minZ - camZ;
        double endZ = maxZ - camZ;

        float u0 = (float) (minX * 0.5D) - timeF;
        float u1 = (float) (maxX * 0.5D) - timeF;
        float v0 = (float) (minZ * 0.5D) - timeF;
        float v1 = (float) (maxZ * 0.5D) - timeF;

        bufferbuilder.vertex(startX, yRender, startZ).uv(u0, v0).endVertex();
        bufferbuilder.vertex(startX, yRender, endZ).uv(u0, v1).endVertex();
        bufferbuilder.vertex(endX, yRender, endZ).uv(u1, v1).endVertex();
        bufferbuilder.vertex(endX, yRender, startZ).uv(u1, v0).endVertex();

        Tesselator.getInstance().end();

        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();

        RenderSystem.setShaderColor(1, 1, 1, 1);

        posestack.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
