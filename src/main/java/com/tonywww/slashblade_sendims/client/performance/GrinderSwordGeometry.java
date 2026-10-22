package com.tonywww.slashblade_sendims.client.performance;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Rotation;
import codechicken.lib.vec.Transformation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.Vertex5;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

/** Renderer-owned immutable meshes, at most the configured byte budget. No per-block cache. */
public final class GrinderSwordGeometry {
    private final double[][] idle = new double[8][];
    private final Vector3 position = new Vector3();
    private final Vector3 normal = new Vector3();
    private CCModel owner;
    private Vertex5[] vertices;
    private Vector3[] normals;
    private double[] source;
    private long epoch = -1;
    private int budget;
    private int bytes;
    private boolean unsupported;
    private boolean rendering;

    public void clear() {
        java.util.Arrays.fill(idle, null);
        owner = null; vertices = null; normals = null; source = null;
        bytes = 0; unsupported = false; epoch = -1;
    }

    private boolean prepare(CCModel model, CCRenderState state) {
        long currentEpoch = RenderResourceEpoch.current();
        int currentBudget = ClientPerformanceOptions.grinderCacheBytes;
        if (owner != model || epoch != currentEpoch || budget != currentBudget) {
            clear(); owner = model; epoch = currentEpoch; budget = currentBudget;
        }
        if (unsupported || rendering || state.fmt != DefaultVertexFormat.NEW_ENTITY
                || state.baseColour != -1 || state.alphaOverride >= 0
                || state.r == null || state.r.getClass() != TransformingVertexConsumer.class) return false;
        if (source != null) {
            if (vertices != model.verts || normals != model.normals()) { clear(); return false; }
            return true;
        }
        if (model.getClass() != CCModel.class || model.vertexMode != VertexFormat.Mode.QUADS
                || model.verts == null || model.verts.length == 0 || model.verts.length % 4 != 0
                || (long) model.verts.length * 64 > budget) { unsupported = true; return false; }
        Vector3[] modelNormals = model.normals();
        if (modelNormals == null || modelNormals.length != model.verts.length) { unsupported = true; return false; }
        for (Object attribute : model.attributes) {
            if (attribute != null && attribute != modelNormals) { unsupported = true; return false; }
        }
        double[] captured = new double[model.verts.length * 8];
        for (int index = 0; index < model.verts.length; index++) {
            Vertex5 vertex = model.verts[index]; Vector3 n = modelNormals[index];
            if (vertex == null || vertex.vec == null || vertex.uv == null || n == null) { unsupported = true; return false; }
            int at = index * 8;
            captured[at] = vertex.vec.x; captured[at + 1] = vertex.vec.y; captured[at + 2] = vertex.vec.z;
            captured[at + 3] = vertex.uv.u; captured[at + 4] = vertex.uv.v;
            captured[at + 5] = n.x; captured[at + 6] = n.y; captured[at + 7] = n.z;
            for (int component = 0; component < 8; component++) {
                if (!Double.isFinite(captured[at + component])) { unsupported = true; return false; }
            }
        }
        source = captured; vertices = model.verts; normals = modelNormals;
        bytes = captured.length * Double.BYTES;
        return true;
    }

    /** Same idle pose as upstream, baked once for each facing/side. */
    public boolean idle(CCModel model, CCRenderState state, int direction, double side, double angle) {
        if (direction < 0 || direction >= 4 || (side != .34 && side != -.34) || !prepare(model, state)) return false;
        int slot = direction * 2 + (side < 0 ? 1 : 0);
        double[] mesh = idle[slot];
        if (mesh == null) {
            if ((long) bytes + source.length * Double.BYTES > budget) return false;
            Vector3 hand = new Vector3(.4975 + side, .5, .125);
            hand.apply(new Rotation(angle, 0, 1, 0).at(new Vector3(.5, 0, .5)));
            Transformation pose = new Rotation(0, 1, 0, 0).at(new Vector3(0, -.2, 0))
                    .with(new Translation(hand)).with(new Rotation(angle, 0, 1, 0).at(hand));
            mesh = transformed(source, pose);
            idle[slot] = mesh; bytes += mesh.length * Double.BYTES;
        }
        emit(mesh, state, null);
        if (ClientPerformanceOptions.diagnostics) RenderOptimizationCounters.grinderIdle++;
        return true;
    }

    public boolean animated(CCModel model, CCRenderState state, IVertexOperation[] operations) {
        if (operations.length != 1 || !(operations[0] instanceof Transformation transform) || !prepare(model, state)) return false;
        // CCL composes the same transformation sequence once instead of visiting it for every vertex.
        emit(source, state, new Matrix4().apply(transform));
        if (ClientPerformanceOptions.diagnostics) RenderOptimizationCounters.grinderAnimated++;
        return true;
    }

    private double[] transformed(double[] data, Transformation transform) {
        double[] result = data.clone();
        for (int at = 0; at < data.length; at += 8) {
            position.set(data[at], data[at + 1], data[at + 2]); transform.apply(position);
            normal.set(data[at + 5], data[at + 6], data[at + 7]); transform.applyN(normal);
            result[at] = position.x; result[at + 1] = position.y; result[at + 2] = position.z;
            result[at + 5] = normal.x; result[at + 6] = normal.y; result[at + 7] = normal.z;
        }
        return result;
    }

    private void emit(double[] data, CCRenderState state, Matrix4 transform) {
        rendering = true;
        try {
            VertexConsumer consumer = state.r;
            for (int at = 0; at < data.length; at += 8) {
                position.set(data[at], data[at + 1], data[at + 2]);
                normal.set(data[at + 5], data[at + 6], data[at + 7]);
                if (transform != null) { transform.apply(position); transform.applyN(normal); }
                consumer.vertex(position.x, position.y, position.z);
                consumer.color(255, 255, 255, 255);
                consumer.uv((float) data[at + 3], (float) data[at + 4]);
                consumer.overlayCoords(state.overlay);
                consumer.uv2(state.brightness);
                consumer.normal((float) normal.x, (float) normal.y, (float) normal.z);
                consumer.endVertex();
            }
        } finally { rendering = false; }
    }

    public int cachedBytes() { return bytes; }
}
