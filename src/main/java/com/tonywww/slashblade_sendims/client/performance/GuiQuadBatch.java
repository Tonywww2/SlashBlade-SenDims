package com.tonywww.slashblade_sendims.client.performance;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** Same quads, colors and ordering as GuiGraphics.fill, without flushing each background slice. */
public final class GuiQuadBatch {
    private GuiQuadBatch() {}

    public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (x1 < x2) { int temporary = x1; x1 = x2; x2 = temporary; }
        if (y1 < y2) { int temporary = y1; y1 = y2; y2 = temporary; }
        if (ClientPerformanceOptions.diagnostics) RenderOptimizationCounters.backgroundSlices++;
        Matrix4f pose = graphics.pose().last().pose();
        VertexConsumer vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        float alpha = (color >>> 24) / 255.0F;
        vertices.vertex(pose, x1, y1, 0).color(red, green, blue, alpha).endVertex();
        vertices.vertex(pose, x1, y2, 0).color(red, green, blue, alpha).endVertex();
        vertices.vertex(pose, x2, y2, 0).color(red, green, blue, alpha).endVertex();
        vertices.vertex(pose, x2, y1, 0).color(red, green, blue, alpha).endVertex();
    }
}
