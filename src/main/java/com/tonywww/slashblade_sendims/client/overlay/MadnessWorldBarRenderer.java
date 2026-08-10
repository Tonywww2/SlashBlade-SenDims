package com.tonywww.slashblade_sendims.client.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.se.FrenziedFlame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MadnessWorldBarRenderer {
    private static final ResourceLocation HUD = SenDims.prefix("textures/gui/madness_bar.png");
    private static final float TEXTURE_WIDTH = 180.0F;
    private static final float TEXTURE_HEIGHT = 25.0F;
    private static final float BAR_WIDTH = 180.0F;
    private static final float BAR_HEIGHT = 18.0F;
    private static final float FILL_WIDTH = 158.0F;
    private static final float FILL_HEIGHT = 7.0F;
    private static final float FILL_X_OFFSET = 18.0F;
    private static final float FILL_Y_OFFSET = 6.0F;
    private static final float PIXEL_SCALE = 0.008F;

    private MadnessWorldBarRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        int madness = entity.getPersistentData().getInt(FrenziedFlame.MADNESS_PATH);
        if (madness == 0) {
            return;
        }

        float ratio = Mth.clamp(madness / Math.max(1.0F, entity.getMaxHealth()), 0.0F, 1.0F);
        float left = -BAR_WIDTH / 2.0F;
        float top = -BAR_HEIGHT / 2.0F;

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.75D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-PIXEL_SCALE, -PIXEL_SCALE, PIXEL_SCALE);

        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(HUD));
        drawTexturedQuad(
                poseStack.last(), consumer,
                left, top, left + BAR_WIDTH, top + BAR_HEIGHT,
                0.0F, 0.0F, BAR_WIDTH / TEXTURE_WIDTH, BAR_HEIGHT / TEXTURE_HEIGHT);

        float renderedFillWidth = FILL_WIDTH * ratio;
        if (renderedFillWidth > 0.0F) {
            float fillLeft = left + FILL_X_OFFSET;
            float fillTop = top + FILL_Y_OFFSET;
            drawTexturedQuad(
                    poseStack.last(), consumer,
                    fillLeft, fillTop, fillLeft + renderedFillWidth, fillTop + FILL_HEIGHT,
                    0.0F, BAR_HEIGHT / TEXTURE_HEIGHT,
                    renderedFillWidth / TEXTURE_WIDTH, 1.0F);
        }

        poseStack.popPose();
    }

    private static void drawTexturedQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                         float left, float top, float right, float bottom,
                                         float minU, float minV, float maxU, float maxV) {
        consumer.vertex(pose.pose(), left, bottom, 0.0F)
                .color(255, 255, 255, 255)
                .uv(minU, maxV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
        consumer.vertex(pose.pose(), right, bottom, 0.0F)
                .color(255, 255, 255, 255)
                .uv(maxU, maxV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
        consumer.vertex(pose.pose(), right, top, 0.0F)
                .color(255, 255, 255, 255)
                .uv(maxU, minV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
        consumer.vertex(pose.pose(), left, top, 0.0F)
                .color(255, 255, 255, 255)
                .uv(minU, minV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0.0F, 0.0F, 1.0F)
                .endVertex();
    }
}