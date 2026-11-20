package com.tonywww.slashblade_sendims.client.performance;

import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.buffer.DelegatingVertexConsumer;
import codechicken.lib.render.buffer.ISpriteAwareVertexConsumer;
import codechicken.lib.render.buffer.TransformingVertexConsumer;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.render.pipeline.attribute.ColourAttribute;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Vector3;
import codechicken.lib.vec.Vertex5;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 龙之研究能量核心、稳定器、能量塔的 CCL 网格直写。
 * CCL 每个顶点都要走一遍通用流水线：复制顶点、按依赖顺序调用各属性操作、再按顶点格式逐元素分支写出。
 * 这里只支持「无变换或单个 Matrix4、只有法线／颜色属性」的普通 CCModel，绑定流水线照常进行，
 * 只把逐顶点部分换成同样的数学（同一个 Matrix4.apply／applyN、同样的 float 转换次序）与同样的格式元素顺序，
 * 写入同一个顶点接收器，Iris 扩展属性仍由 BufferBuilder 自己补齐。
 * CCL 的 TransformingVertexConsumer（PoseStack 包装）拆开后按它自己的顺序先变换再交给内层，省掉一层委托。
 * 每帧读取实时模型数据，不缓存；遇到任何不认识的模型、操作、接收器或格式元素都交回 CCL 原实现。
 */
public final class CCModelGeometry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final byte SKIP = 0, POS = 1, TEX = 2, OVERLAY = 3, LIGHTMAP = 4, COL = 5, NRM = 6;

    // 三个字段在类加载时用反射各取一次。不能用 Mixin 访问器：访问器会让 BufferBuilder 的父类成为 Mixin 目标，
    // Mixin 随之改写 Oculus 对 BufferVertexConsumer.super.uv2 的调用，生成非法字节码，一渲染文字就崩溃（2026-11-18）。
    private static final MethodHandle DEFAULT_COLOR_SET = getter(DefaultedVertexConsumer.class, "f_85824_", true);
    private static final MethodHandle TRANSFORM = getter(TransformingVertexConsumer.class, "transform", false);
    private static final MethodHandle DELEGATE = getter(DelegatingVertexConsumer.class, "delegate", false);

    private CCModelGeometry() {}

    private static MethodHandle getter(Class<?> owner, String name, boolean srgName) {
        try {
            Field field = srgName ? ObfuscationReflectionHelper.findField(owner, name) : owner.getDeclaredField(name);
            field.setAccessible(true);
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (ReflectiveOperationException | RuntimeException error) {
            LOGGER.warn("Cannot read {}.{}; Draconic meshes on that path keep CCL rendering", owner.getName(), name, error);
            return null;
        }
    }

    private static Object read(MethodHandle getter, Object target) {
        try {
            return getter.invoke(target);
        } catch (RuntimeException | Error error) {
            throw error;
        } catch (Throwable error) {
            throw new IllegalStateException(error);
        }
    }

    public static void render(CCModel model, CCRenderState state, IVertexOperation[] operations) {
        if (!ClientPerformanceOptions.draconicModelGeometry || !emit(model, state, operations)) {
            model.render(state, operations);
        }
    }

    private static boolean emit(CCModel model, CCRenderState state, IVertexOperation[] operations) {
        Matrix4 inner = null;
        if (operations.length == 1 && operations[0] != null && operations[0].getClass() == Matrix4.class) {
            inner = (Matrix4) operations[0];
        } else if (operations.length != 0) {
            return false;
        }
        Vertex5[] vertices = model.verts;
        VertexConsumer consumer = state.r;
        if (model.getClass() != CCModel.class || vertices == null || consumer == null || state.fmt == null) return false;
        byte[] layout = layout(state.fmt.getElements());
        if (layout == null) return false;

        VertexConsumer target = consumer;
        Matrix4 outer = null;
        if (consumer.getClass() == TransformingVertexConsumer.class) {
            if (TRANSFORM == null || DELEGATE == null) return false;
            Object transform = read(TRANSFORM, consumer);
            Object delegate = read(DELEGATE, consumer);
            if (transform == null || transform.getClass() != Matrix4.class || !(delegate instanceof VertexConsumer inside)
                    || inside instanceof ISpriteAwareVertexConsumer) return false;
            outer = (Matrix4) transform;
            target = inside;
        } else if (consumer instanceof ISpriteAwareVertexConsumer) {
            return false;
        }
        boolean defaultColour = false;
        if (consumer instanceof BufferBuilder) {
            if (DEFAULT_COLOR_SET == null) return false;
            defaultColour = (Boolean) read(DEFAULT_COLOR_SET, consumer);
        }

        // 与 CCModel.render 相同的模型绑定和属性选择；只替换逐顶点部分。
        state.setPipeline(model, 0, vertices.length, operations);
        if (state.lightingAttrib.active || state.sideAttrib.active || state.lightCoordAttrib.active) return false;
        boolean hasNormal = state.normalAttrib.active;
        boolean hasColour = state.colourAttrib.active;
        Vector3[] normals = hasNormal ? model.normals() : null;
        if (hasNormal && (normals == null || normals.length < vertices.length)) return false;
        int[] colours = hasColour ? model.getAttribute(ColourAttribute.attributeKey) : null;
        if (colours != null && colours.length < vertices.length) return false;

        int base = state.baseColour;
        int alpha = state.alphaOverride;
        int overlay = state.overlay;
        int light = state.brightness;
        int colour = state.colour;
        Vector3 position = new Vector3();
        Vector3 normal = new Vector3();
        Vector3 outerNormal = new Vector3();
        for (int index = 0; index < vertices.length; index++) {
            Vertex5 vertex = vertices[index];
            position.set(vertex.vec);
            if (hasNormal) normal.set(normals[index]);
            if (inner != null) {
                inner.apply(position);
                if (hasNormal) inner.applyN(normal);
            }
            if (hasColour) colour = colours != null ? ColourRGBA.multiply(base, colours[index]) : base;
            for (byte element : layout) {
                switch (element) {
                    case POS -> {
                        if (outer != null) outer.apply(position);
                        target.vertex(position.x, position.y, position.z);
                    }
                    case TEX -> target.uv((float) vertex.uv.u, (float) vertex.uv.v);
                    case OVERLAY -> target.overlayCoords(overlay);
                    case LIGHTMAP -> target.uv2(light);
                    case COL -> {
                        if (defaultColour) {
                            ((BufferBuilder) target).nextElement();
                        } else {
                            target.color(colour >>> 24, colour >> 16 & 0xFF, colour >> 8 & 0xFF, alpha >= 0 ? alpha : colour & 0xFF);
                        }
                    }
                    case NRM -> {
                        if (outer != null) {
                            // TransformingVertexConsumer 收到的是 float 法线，再变换、归一化后转回 float。
                            outerNormal.set((float) normal.x, (float) normal.y, (float) normal.z);
                            outer.applyN(outerNormal);
                            target.normal((float) outerNormal.x, (float) outerNormal.y, (float) outerNormal.z);
                        } else {
                            target.normal((float) normal.x, (float) normal.y, (float) normal.z);
                        }
                    }
                    default -> {
                    }
                }
            }
            target.endVertex();
        }
        state.vertexIndex = vertices.length;
        state.colour = colour;
        return true;
    }

    private static byte[] layout(List<VertexFormatElement> elements) {
        byte[] layout = new byte[elements.size()];
        for (int index = 0; index < layout.length; index++) {
            VertexFormatElement element = elements.get(index);
            switch (element.getUsage()) {
                case POSITION -> layout[index] = POS;
                case UV -> {
                    int uvIndex = element.getIndex();
                    layout[index] = uvIndex == 0 ? TEX : uvIndex == 1 ? OVERLAY : uvIndex == 2 ? LIGHTMAP : SKIP;
                }
                case COLOR -> layout[index] = COL;
                case NORMAL -> layout[index] = NRM;
                case PADDING -> layout[index] = SKIP;
                default -> {
                    return null;
                }
            }
        }
        return layout;
    }
}
