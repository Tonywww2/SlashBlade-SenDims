package com.tonywww.slashblade_sendims.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.entities.EntityMapleLeaf;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** A spinning, camera-facing red maple leaf. */
public final class MapleLeafRenderer extends EntityRenderer<EntityMapleLeaf> {
    private static final ResourceLocation TEXTURE = SenDims.prefix("textures/effect/maple_leaf.png");
    private static final float SIZE = 0.5F;

    public MapleLeafRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public ResourceLocation getTextureLocation(EntityMapleLeaf entity) { return TEXTURE; }

    @Override
    public void render(EntityMapleLeaf entity, float yaw, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, 0.12F, 0.0F);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        float spin = (entity.tickCount + partialTicks) * 24.0F + (entity.getId() % 360) * 37.0F;
        pose.mulPose(Axis.ZP.rotationDegrees(spin));
        pose.scale(SIZE, SIZE, SIZE);
        PoseStack.Pose last = pose.last();
        Matrix4f matrix = last.pose();
        Matrix3f normal = last.normal();
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        vertex(out, matrix, normal, -0.5F, -0.5F, 0.0F, 1.0F);
        vertex(out, matrix, normal, 0.5F, -0.5F, 1.0F, 1.0F);
        vertex(out, matrix, normal, 0.5F, 0.5F, 1.0F, 0.0F);
        vertex(out, matrix, normal, -0.5F, 0.5F, 0.0F, 0.0F);
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                               float x, float y, float u, float v) {
        out.vertex(matrix, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
