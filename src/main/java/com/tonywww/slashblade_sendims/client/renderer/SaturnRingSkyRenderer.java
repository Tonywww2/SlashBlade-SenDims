package com.tonywww.slashblade_sendims.client.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.tonywww.slashblade_sendims.SenDims;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.Random;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SaturnRingSkyRenderer {

    private static final ResourceLocation SATURN_RING_DIMENSION = SenDims.prefix("saturn_ring");
    private static final ResourceLocation SKY_TEXTURE = SenDims.prefix("textures/environment/saturn_sky.png");
    private static final ResourceLocation STARS_TEXTURE = SenDims.prefix("textures/environment/saturn_stars.png");
    private static final ResourceLocation NEBULA_1_TEXTURE = SenDims.prefix("textures/environment/saturn_nebula_1.png");
    private static final ResourceLocation NEBULA_2_TEXTURE = SenDims.prefix("textures/environment/saturn_nebula_2.png");
    private static final ResourceLocation HORIZON_DUST_TEXTURE = SenDims.prefix("textures/environment/saturn_horizon_dust.png");
    private static final ResourceLocation PLANET_TEXTURE = SenDims.prefix("textures/environment/saturn_planet.png");
    private static final ResourceLocation RING_SOFT_TEXTURE = SenDims.prefix("textures/environment/saturn_ring_soft.png");
    private static final ResourceLocation RING_SHARP_TEXTURE = SenDims.prefix("textures/environment/saturn_ring_sharp.png");

    private static final double SKY_RADIUS = 96.0D;
    private static final Star[] STARS = createStars();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || !level.dimension().location().equals(SATURN_RING_DIMENSION)) return;

        double time = level.getGameTime() + event.getPartialTick();
        double playerY = mc.player == null ? 96.0D : mc.player.position().y;
        float ringAngle = Mth.clamp((float) (playerY - 96.0D) * 0.0006F, -0.03F, 0.03F);
        float dustAlpha = 0.35F + 0.15F * Mth.sin((float) time * 0.003F);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO
        );
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        renderBackground(poseStack);
        renderStars(poseStack, (float) time);
        renderNebula(poseStack, (float) time);
        renderHorizonDust(poseStack, (float) time, dustAlpha);
        renderRingLayer(poseStack, RING_SOFT_TEXTURE, -100.0F, 130.0F, ringAngle, 0.75F);
        renderPlanet(poseStack);
        renderRingLayer(poseStack, RING_SHARP_TEXTURE, -100.0F, 130.0F, ringAngle, 0.42F);

        poseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderBackground(PoseStack poseStack) {
        RenderSystem.setShaderTexture(0, SKY_TEXTURE);
        drawCube(poseStack, SKY_RADIUS, 0.08F, 0.10F, 0.18F, 1.0F);
    }

    private static void renderStars(PoseStack poseStack, float time) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.008F));
        RenderSystem.setShaderTexture(0, STARS_TEXTURE);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Star star : STARS) {
            drawStar(buffer, matrix, star);
        }
        Tesselator.getInstance().end();
        poseStack.popPose();
    }

    private static void renderNebula(PoseStack poseStack, float time) {
        RenderSystem.setShaderTexture(0, NEBULA_1_TEXTURE);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-28.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(35.0F + time * 0.003F));
        drawBillboard(poseStack, 0.0F, 12.0F, -92.0F, 150.0F, 0.58F, 0.55F, 0.82F, 0.24F);
        poseStack.popPose();

        RenderSystem.setShaderTexture(0, NEBULA_2_TEXTURE);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-60.0F - time * 0.002F));
        drawBillboard(poseStack, 0.0F, -8.0F, -94.0F, 130.0F, 0.35F, 0.50F, 0.70F, 0.18F);
        poseStack.popPose();
    }

    private static void renderHorizonDust(PoseStack poseStack, float time, float alpha) {
        RenderSystem.setShaderTexture(0, HORIZON_DUST_TEXTURE);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.010F));
        drawCylinder(poseStack, 88.0F, 28.0F, 20, 0.42F, 0.46F, 0.58F, alpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 0.006F));
        drawCylinder(poseStack, 92.0F, 46.0F, 20, 0.25F, 0.28F, 0.38F, alpha * 0.55F);
        poseStack.popPose();
    }

    private static void renderPlanet(PoseStack poseStack) {
        RenderSystem.setShaderTexture(0, PLANET_TEXTURE);
        poseStack.pushPose();
        poseStack.translate(0.0D, 18.0D, -100.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-3.0F));
        drawBillboard(poseStack, 0.0F, 0.0F, 0.0F, 118.0F, 1.0F, 1.0F, 1.0F, 0.92F);
        poseStack.popPose();
    }

    private static void renderRingLayer(PoseStack poseStack, ResourceLocation texture, float z, float size, float angle, float alpha) {
        RenderSystem.setShaderTexture(0, texture);
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, z);
        poseStack.mulPose(Axis.XP.rotation(angle));
        drawFlatQuad(poseStack, size, 0.86F, 0.90F, 1.0F, alpha);
        poseStack.popPose();
    }

    private static void drawCube(PoseStack poseStack, double radius, float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        double min = -radius;
        double max = radius;
        face(buffer, matrix, min, max, min, max, max, min, max, max, max, min, max, max, r, g, b, a);
        face(buffer, matrix, max, min, min, min, min, min, min, min, max, max, min, max, r, g, b, a);
        face(buffer, matrix, min, min, max, min, max, max, max, max, max, max, min, max, r, g, b, a);
        face(buffer, matrix, max, min, min, max, max, min, min, max, min, min, min, min, r, g, b, a);
        face(buffer, matrix, min, min, min, min, min, max, min, max, max, min, max, min, r, g, b, a);
        face(buffer, matrix, max, min, max, max, min, min, max, max, min, max, max, max, r, g, b, a);

        Tesselator.getInstance().end();
    }

    private static void drawFlatQuad(PoseStack poseStack, float size, float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, -size, 0.0F, -size).uv(0.0F, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, size, 0.0F, -size).uv(1.0F, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, size, 0.0F, size).uv(1.0F, 1.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, -size, 0.0F, size).uv(0.0F, 1.0F).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
    }

    private static void drawBillboard(PoseStack poseStack, float x, float y, float z, float size, float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(matrix, x - size, y + size, z).uv(0.0F, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x + size, y + size, z).uv(1.0F, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x + size, y - size, z).uv(1.0F, 1.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x - size, y - size, z).uv(0.0F, 1.0F).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
    }

    private static void drawCylinder(PoseStack poseStack, float radius, float height, int segments, float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < segments; i++) {
            double a1 = i * Math.PI * 2.0D / segments;
            double a2 = (i + 1) * Math.PI * 2.0D / segments;
            float x1 = (float) (Math.sin(a1) * radius);
            float z1 = (float) (Math.cos(a1) * radius);
            float x2 = (float) (Math.sin(a2) * radius);
            float z2 = (float) (Math.cos(a2) * radius);
            float u0 = (float) i / segments;
            float u1 = (float) (i + 1) / segments;

            buffer.vertex(matrix, x1, -height, z1).uv(u0, 1.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x1, height, z1).uv(u0, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x2, height, z2).uv(u1, 0.0F).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x2, -height, z2).uv(u1, 1.0F).color(r, g, b, a).endVertex();
        }
        Tesselator.getInstance().end();
    }

    private static void face(BufferBuilder buffer,
                             Matrix4f matrix,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             double x3, double y3, double z3,
                             double x4, double y4, double z4,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z1).uv(0.0F, 0.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, (float) x2, (float) y2, (float) z2).uv(0.0F, 1.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, (float) x3, (float) y3, (float) z3).uv(1.0F, 1.0F).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, (float) x4, (float) y4, (float) z4).uv(1.0F, 0.0F).color(r, g, b, a).endVertex();
    }

    private static void drawStar(BufferBuilder buffer, Matrix4f matrix, Star star) {
        double px = star.x * 92.0D;
        double py = star.y * 92.0D;
        double pz = star.z * 92.0D;

        double upX = Math.abs(star.y) > 0.85D ? 1.0D : 0.0D;
        double upY = Math.abs(star.y) > 0.85D ? 0.0D : 1.0D;
        double upZ = 0.0D;

        double rightX = upY * star.z - upZ * star.y;
        double rightY = upZ * star.x - upX * star.z;
        double rightZ = upX * star.y - upY * star.x;
        double rightLength = 1.0D / Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        rightX *= rightLength;
        rightY *= rightLength;
        rightZ *= rightLength;

        double localUpX = star.y * rightZ - star.z * rightY;
        double localUpY = star.z * rightX - star.x * rightZ;
        double localUpZ = star.x * rightY - star.y * rightX;
        double size = star.size;

        vertex(buffer, matrix, px - rightX * size - localUpX * size, py - rightY * size - localUpY * size, pz - rightZ * size - localUpZ * size, 0.0F, 1.0F, star.alpha);
        vertex(buffer, matrix, px + rightX * size - localUpX * size, py + rightY * size - localUpY * size, pz + rightZ * size - localUpZ * size, 1.0F, 1.0F, star.alpha);
        vertex(buffer, matrix, px + rightX * size + localUpX * size, py + rightY * size + localUpY * size, pz + rightZ * size + localUpZ * size, 1.0F, 0.0F, star.alpha);
        vertex(buffer, matrix, px - rightX * size + localUpX * size, py - rightY * size + localUpY * size, pz - rightZ * size + localUpZ * size, 0.0F, 0.0F, star.alpha);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z, float u, float v, float alpha) {
        buffer.vertex(matrix, (float) x, (float) y, (float) z).uv(u, v).color(0.86F, 0.91F, 1.0F, alpha).endVertex();
    }

    private static Star[] createStars() {
        Random random = new Random(41315L);
        Star[] stars = new Star[900];
        for (int i = 0; i < stars.length; i++) {
            double x;
            double y;
            double z;
            double length;
            do {
                x = random.nextDouble() * 2.0D - 1.0D;
                y = random.nextDouble() * 2.0D - 1.0D;
                z = random.nextDouble() * 2.0D - 1.0D;
                length = x * x + y * y + z * z;
            }
            while (length > 1.0D || length < 0.001D);

            double scale = 1.0D / Math.sqrt(length);
            float size = 0.09F + random.nextFloat() * 0.42F;
            float alpha = 0.35F + random.nextFloat() * 0.65F;
            stars[i] = new Star(x * scale, y * scale, z * scale, size, alpha);
        }
        return stars;
    }

    private record Star(double x, double y, double z, float size, float alpha) {
    }
}
