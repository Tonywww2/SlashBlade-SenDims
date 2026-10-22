package com.tonywww.slashblade_sendims.client.performance;

import com.mojang.blaze3d.vertex.VertexConsumer;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Face;
import mods.flammpfeil.slashblade.client.renderer.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.renderer.model.obj.Vertex;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Reads live face data. Only temporary math objects are reused; custom alpha callbacks fall back. */
public final class ObjFaceEmitter {
    private final Matrix3f normalMatrix = new Matrix3f();
    private final Vector3f normal = new Vector3f();
    private boolean rendering;

    public boolean render(Face face, VertexConsumer consumer, float offset, Matrix4f pose, int light, int color) {
        if (rendering || face.getClass() != Face.class || Face.alphaOverride != Face.alphaNoOverride
                || face.vertices == null || face.vertices.length < 3
                || (face.textureCoordinates != null && face.textureCoordinates.length != 0
                    && face.textureCoordinates.length < face.vertices.length)
                || (face.vertexNormals != null && face.vertexNormals.length < face.vertices.length)) return false;
        rendering = true;
        try {
            if (face.faceNormal == null) face.faceNormal = face.calculateFaceNormal();
            normalMatrix.set(pose);
            TextureCoordinate[] texture = face.textureCoordinates;
            Vector4f uv = Face.uvOperator;
            float averageU = 0, averageV = 0;
            if (texture != null && texture.length > 0) {
                for (TextureCoordinate coordinate : texture) {
                    averageU += coordinate.u * uv.x() + uv.z();
                    averageV += coordinate.v * uv.y() + uv.w();
                }
                averageU /= texture.length;
                averageV /= texture.length;
            }
            int red = color >> 16 & 255, green = color >> 8 & 255, blue = color & 255, alpha = color >>> 24;
            for (int index = 0; index < face.vertices.length; index++) {
                Vertex vertex = face.vertices[index];
                consumer.vertex(pose, vertex.x, vertex.y, vertex.z);
                consumer.color(red, green, blue, alpha);
                if (texture != null && texture.length > 0) {
                    float u = texture[index].u * uv.x() + uv.z();
                    float v = texture[index].v * uv.y() + uv.w();
                    consumer.uv(u + (u > averageU ? -offset : offset), v + (v > averageV ? -offset : offset));
                } else consumer.uv(0, 0);
                consumer.overlayCoords(OverlayTexture.NO_OVERLAY);
                consumer.uv2(light);
                Vertex sourceNormal = face.vertexNormals == null ? face.faceNormal : face.vertexNormals[index];
                normal.set(sourceNormal.x, sourceNormal.y, sourceNormal.z).mul(normalMatrix).normalize();
                consumer.normal(normal.x, normal.y, normal.z);
                consumer.endVertex();
            }
            if (ClientPerformanceOptions.diagnostics) RenderOptimizationCounters.objFaces++;
            return true;
        } finally {
            rendering = false;
        }
    }
}
