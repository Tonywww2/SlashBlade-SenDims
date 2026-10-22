package com.tonywww.slashblade_sendims.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.entities.EntityHolySwordLight;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Full-bright solid light volumes using the vanilla depth-tested lightning pass. */
public final class HolySwordLightRenderer extends EntityRenderer<EntityHolySwordLight> {
    public HolySwordLightRenderer(EntityRendererProvider.Context context) { super(context); }

    @Override public ResourceLocation getTextureLocation(EntityHolySwordLight entity) {
        return SenDims.prefix("textures/effect/holy_sword_light.png");
    }

    @Override public void render(EntityHolySwordLight entity, float yaw, float partialTicks,
                                 PoseStack pose, MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partialTicks;
        float opacity = Math.min(1.0F, Math.max(0.0F, (entity.lifetime() - age) / 4.0F));
        if (entity.length() <= 0.01F || opacity <= 0.0F) return;
        pose.pushPose();
        var direction = entity.direction();
        pose.mulPose(new Quaternionf().rotateTo(new Vector3f(0, 0, 1),
                new Vector3f((float) direction.x, (float) direction.y, (float) direction.z)));
        Matrix4f matrix = pose.last().pose();
        VertexConsumer vertices = buffers.getBuffer(RenderType.lightning());
        float radius = entity.width() * 0.5F;
        float vertical = entity.wave() ? 0.28F : 1.0F;
        float shimmer = 1.0F + 0.035F * (float) Math.sin(age * 1.8F);
        tube(vertices, matrix, 0, entity.length(), radius * shimmer, vertical, 255, 227, 166, (int) (42 * opacity));
        tube(vertices, matrix, 0, entity.length(), radius * 0.72F, vertical, 255, 240, 194, (int) (110 * opacity));
        tube(vertices, matrix, 0, entity.length(), radius * 0.38F, vertical, 255, 253, 235, (int) (225 * opacity));
        tube(vertices, matrix, 0, entity.length(), radius * 0.13F, vertical, 255, 255, 255, (int) (255 * opacity));
        for (int i = 0; i < 5; i++) {
            float z = (age * 2.4F + i * entity.length() / 5.0F) % entity.length();
            tube(vertices, matrix, z, Math.min(entity.length(), z + 0.13F), radius * 1.02F,
                    vertical, 255, 245, 214, (int) (125 * opacity));
        }
        pose.popPose();
        super.render(entity, yaw, partialTicks, pose, buffers, light);
    }

    private static void tube(VertexConsumer out, Matrix4f matrix, float start, float end, float radius,
                              float vertical, int r, int g, int b, int a) {
        int sides = 20;
        for (int i = 0; i < sides; i++) {
            double first = i * Math.PI * 2.0D / sides;
            double second = (i + 1) * Math.PI * 2.0D / sides;
            float x1 = (float) Math.cos(first) * radius, y1 = (float) Math.sin(first) * radius * vertical;
            float x2 = (float) Math.cos(second) * radius, y2 = (float) Math.sin(second) * radius * vertical;
            vertex(out, matrix, x1, y1, start, r, g, b, a); vertex(out, matrix, x2, y2, start, r, g, b, a);
            vertex(out, matrix, x2, y2, end, r, g, b, a); vertex(out, matrix, x1, y1, end, r, g, b, a);
            // End caps also make the light visible head-on.
            vertex(out, matrix, 0, 0, start, r, g, b, a); vertex(out, matrix, x2, y2, start, r, g, b, a);
            vertex(out, matrix, x1, y1, start, r, g, b, a); vertex(out, matrix, 0, 0, start, r, g, b, a);
            vertex(out, matrix, 0, 0, end, r, g, b, a); vertex(out, matrix, x1, y1, end, r, g, b, a);
            vertex(out, matrix, x2, y2, end, r, g, b, a); vertex(out, matrix, 0, 0, end, r, g, b, a);
        }
    }

    private static void vertex(VertexConsumer out, Matrix4f matrix, float x, float y, float z,
                                int r, int g, int b, int a) {
        out.vertex(matrix, x, y, z).color(r, g, b, a).endVertex();
    }
}
