package com.tonywww.slashblade_sendims.client.zenith;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.config.SenDimsConfig;
import com.tonywww.slashblade_sendims.sa.ZenithRiftArts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 真·次元斩的屏幕层，由 GameRenderer Mixin 在 HUD 画完之后调用（隐藏界面时改在 renderLevel 之后），
 * 所以小地图、血条、快捷栏等 HUD 也会一起定格、碎裂；打开的界面（暂停菜单、背包、聊天）画在它上面，照常可用。
 * <ul>
 *   <li>0–40 tick：斩击只画在世界里（{@link ZenithRiftClient}）。屏幕层只加暗角、褪色，
 *       并把世界斩痕按镜头投影，在斩痕两侧做局部的反向错位：错位宽度和强度随距离透视缩放，近处的斩痕切开的空间更宽。</li>
 *   <li>40–60 tick：把当时的整帧画面（含 HUD）拷进定格图，此后显示定格图（褪色、偏冷）。
 *       收刀前（54–59 tick），裂纹从定格画面里的斩痕处沿斩痕方向向两端裂开，贯穿全屏。</li>
 *   <li>60–80 tick：沿这些裂纹把屏幕切成凸多边形碎片，碎片带着定格画面飞散、坠落，
 *       碎片后面直接是实时画面（不画虚空底色，不黑屏），白光闪过。</li>
 * </ul>
 * 线段数据通过 64×2 的浮点纹理传给着色器。任何一步失败都只关掉屏幕层，不影响伤害和世界层。
 */
public final class ZenithRiftScreen {
    /** 碎裂裂纹最多 24 条；定格画面里看得到的斩痕不足 10 条时（例如背对施放范围），用随机裂纹补足。 */
    private static final int MAX_CUTS = 24;
    private static final int MIN_CUTS = 10;
    private static final int MAX_LINES = 64;
    private static final int MAX_WORLD_SEGMENTS = MAX_LINES - MAX_CUTS;
    private static final double CRACK_START = 54.0;
    private static final double CRACK_TIME = 5.0;
    private static final int MAX_SHARDS = 400;
    private static final float NEAR_W = 0.05F;

    private static ZenithRiftClient.Rift rift;
    /** 随机补足用的裂纹（种子决定）：经过的点和像素空间里的角度。 */
    private static final float[] RANDOM_X = new float[MAX_CUTS];
    private static final float[] RANDOM_Y = new float[MAX_CUTS];
    private static final float[] RANDOM_ANGLE = new float[MAX_CUTS];
    private static final float[] CUT_AMP = new float[MAX_CUTS];
    /** 碎裂裂纹：起点（斩痕中点，uv）、方向（uv）、贯穿全屏的两个端点（uv）。 */
    private static final float[] CUT_X = new float[MAX_CUTS];
    private static final float[] CUT_Y = new float[MAX_CUTS];
    private static final float[] CUT_DX = new float[MAX_CUTS];
    private static final float[] CUT_DY = new float[MAX_CUTS];
    private static final float[] CUT_ENDS = new float[MAX_CUTS * 4];
    private static int cutCount;
    /** 定格时的世界斩痕投影：每条 8 个数（a.xy、b.xy、完整度、亮度、错位、宽度）。 */
    private static final float[] FROZEN_SEGMENTS = new float[MAX_WORLD_SEGMENTS * 8];
    private static int frozenSegmentCount;
    private static final float[] LIVE_SEGMENTS = new float[MAX_WORLD_SEGMENTS * 8];
    private static final float[] ROW = new float[8];
    private static final float[] SHARD_X = new float[32];
    private static final float[] SHARD_Y = new float[32];

    private static final Matrix4f PROJECTION = new Matrix4f();
    private static final Matrix4f VIEW = new Matrix4f();
    private static final Vector4f CLIP_A = new Vector4f();
    private static final Vector4f CLIP_B = new Vector4f();
    private static Vec3 camera = Vec3.ZERO;

    private static TextureTarget copy;
    private static TextureTarget frozen;
    private static TextureTarget processed;
    private static boolean frozenTaken;
    private static boolean processedReady;
    private static List<Shard> shards;
    private static int lineTexture = -1;
    private static FloatBuffer lineData;
    private static boolean failed;

    private ZenithRiftScreen() {
    }

    /** 一块碎片：屏幕 uv 下的凸多边形，加上飞散参数。 */
    private record Shard(float[] xs, float[] ys, float cx, float cy, float dirX, float dirY, float speed,
            float spin, float gravity) {
    }

    static void start(ZenithRiftClient.Rift next) {
        rift = next;
        frozenTaken = false;
        processedReady = false;
        frozenSegmentCount = 0;
        cutCount = 0;
        shards = null;
        Random random = new Random(next.seed ^ 0x7C3A9E15D2B4F681L);
        for (int i = 0; i < MAX_CUTS; i++) {
            RANDOM_X[i] = 0.12F + random.nextFloat() * 0.76F;
            RANDOM_Y[i] = 0.12F + random.nextFloat() * 0.76F;
            RANDOM_ANGLE[i] = random.nextFloat() * (float) Math.PI;
            CUT_AMP[i] = (0.0020F + random.nextFloat() * 0.0035F) * (random.nextBoolean() ? 1.0F : -1.0F);
        }
    }

    /** 结束屏幕层；{@code which} 为 null 时无条件结束。 */
    static void stop(ZenithRiftClient.Rift which) {
        if (which != null && which != rift) return;
        rift = null;
        shards = null;
        frozenTaken = false;
        processedReady = false;
        if (!RenderSystem.isOnRenderThread()) return;
        if (copy != null) copy.destroyBuffers();
        if (frozen != null) frozen.destroyBuffers();
        if (processed != null) processed.destroyBuffers();
        copy = frozen = processed = null;
    }

    static void captureCamera(Matrix4f projection, Matrix4f view, Vec3 position) {
        PROJECTION.set(projection);
        VIEW.set(view);
        camera = position;
    }

    public static boolean active() {
        return rift != null;
    }

    /** GameRenderer Mixin 调用。 */
    public static void render(float partialTick) {
        if (rift == null) return;
        Minecraft mc = Minecraft.getInstance();
        ShaderInstance shader = ZenithRiftShaders.screen;
        if (mc.level == null || failed || shader == null || rift.ended || rift.level != mc.level
                || !SenDimsConfig.ZENITH_ENABLED.get() || !SenDimsConfig.ZENITH_SCREEN_EFFECTS.get()) {
            stop(null);
            return;
        }
        double t = rift.time(partialTick);
        if (t >= ZenithRiftArts.DURATION) {
            stop(null);
            return;
        }
        try {
            draw(mc, shader, t);
        } catch (RuntimeException error) {
            failed = true;
            restoreState();
            stop(null);
            SenDims.LOGGER.warn("True Judgement Cut screen effect failed; screen effects are disabled for this session", error);
        }
    }

    private static void draw(Minecraft mc, ShaderInstance shader, double t) {
        RenderTarget main = mc.getMainRenderTarget();
        int width = main.width;
        int height = main.height;
        float aspect = (float) width / Math.max(1, height);
        if (t < ZenithRiftArts.SHATTER && (t < ZenithRiftArts.FREEZE || !frozenTaken)) {
            copy = ensure(copy, width, height);
            blit(main, copy);
        }
        if (t < ZenithRiftArts.FREEZE) {
            int count = fillLines(t, true);
            process(shader, copy, main, t, count, aspect);
        } else if (t < ZenithRiftArts.SHATTER) {
            freezeFrame(aspect);
            int count = fillLines(t, false);
            process(shader, frozen, main, t, count, aspect);
            // 碎片的纹理是最后一帧处理过的定格画面：画完后从主缓冲拷一份（界面还没画）。
            processed = ensure(processed, width, height);
            blit(main, processed);
            processedReady = true;
            main.bindWrite(true);
        } else {
            if (!processedReady) {
                // 定格阶段一帧都没画到（例如窗口最小化），用当前画面当碎片。
                processed = ensure(processed, width, height);
                blit(main, processed);
                processedReady = true;
                main.bindWrite(true);
            }
            if (shards == null) shards = split(aspect);
            shatter(main, t, width, height);
        }
        flash(t);
        restoreState();
    }

    /** 定格：本帧的画面已经由乱斩阶段一直在用的拷贝缓冲接住，直接把它换成定格图，不另建缓冲再拷。 */
    private static void freezeFrame(float aspect) {
        if (frozenTaken) return;
        TextureTarget spare = frozen;
        frozen = copy;
        copy = spare;
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        frozenSegmentCount = projectWorld(ZenithRiftArts.FREEZE - 0.01, FROZEN_SEGMENTS);
        for (int i = 0; i < frozenSegmentCount; i++) {
            FROZEN_SEGMENTS[i * 8 + 5] = 0.35F;
        }
        buildCuts(aspect);
        frozenTaken = true;
    }

    /** 裂纹取定格画面里屏幕上最长的斩痕，沿斩痕方向延长到贯穿全屏；不足时用随机裂纹补足。 */
    private static void buildCuts(float aspect) {
        cutCount = 0;
        Integer[] order = new Integer[frozenSegmentCount];
        float[] length = new float[frozenSegmentCount];
        for (int i = 0; i < frozenSegmentCount; i++) {
            order[i] = i;
            float ex = (FROZEN_SEGMENTS[i * 8 + 2] - FROZEN_SEGMENTS[i * 8]) * aspect;
            float ey = FROZEN_SEGMENTS[i * 8 + 3] - FROZEN_SEGMENTS[i * 8 + 1];
            length[i] = ex * ex + ey * ey;
        }
        java.util.Arrays.sort(order, (a, b) -> Float.compare(length[b], length[a]));
        for (int k = 0; k < order.length && cutCount < MAX_CUTS; k++) {
            int o = order[k] * 8;
            float ax = FROZEN_SEGMENTS[o];
            float ay = FROZEN_SEGMENTS[o + 1];
            float bx = FROZEN_SEGMENTS[o + 2];
            float by = FROZEN_SEGMENTS[o + 3];
            float mx = Math.min(0.98F, Math.max(0.02F, (ax + bx) * 0.5F));
            float my = Math.min(0.98F, Math.max(0.02F, (ay + by) * 0.5F));
            addCut(mx, my, bx - ax, by - ay);
        }
        for (int i = 0; cutCount < MIN_CUTS && i < MAX_CUTS; i++) {
            addCut(RANDOM_X[i], RANDOM_Y[i], (float) Math.cos(RANDOM_ANGLE[i]) / aspect, (float) Math.sin(RANDOM_ANGLE[i]));
        }
    }

    private static void addCut(float x, float y, float dx, float dy) {
        float[] ends = ROW;
        if (!clipLine(x, y, dx, dy, ends)) return;
        CUT_X[cutCount] = x;
        CUT_Y[cutCount] = y;
        CUT_DX[cutCount] = dx;
        CUT_DY[cutCount] = dy;
        System.arraycopy(ends, 0, CUT_ENDS, cutCount * 4, 4);
        cutCount++;
    }

    // ---------------------------------------------------------------- 线段数据

    /**
     * 填写线段纹理，返回线段数。live 为 true 时只有按当前镜头投影的世界斩痕；
     * 定格阶段用定格时的投影，收刀前再加上从斩痕处向两端裂开的裂纹。
     */
    private static int fillLines(double t, boolean live) {
        FloatBuffer data = lineData();
        for (int i = 0; i < data.capacity(); i++) data.put(i, 0.0F);
        int count = 0;
        float[] row = ROW;
        if (!live && t >= CRACK_START) {
            float r = (float) ZenithRiftClient.smooth((t - CRACK_START) / CRACK_TIME);
            for (int i = 0; i < cutCount; i++) {
                float x = CUT_X[i];
                float y = CUT_Y[i];
                row[0] = x + (CUT_ENDS[i * 4] - x) * r;
                row[1] = y + (CUT_ENDS[i * 4 + 1] - y) * r;
                row[2] = x + (CUT_ENDS[i * 4 + 2] - x) * r;
                row[3] = y + (CUT_ENDS[i * 4 + 3] - y) * r;
                row[4] = r;
                row[5] = 0.9F + 0.6F * r;
                row[6] = CUT_AMP[i] * r;
                row[7] = 0.0F;
                put(data, count++, row);
            }
        }
        if (live) {
            int n = projectWorld(t, LIVE_SEGMENTS);
            for (int i = 0; i < n; i++) {
                System.arraycopy(LIVE_SEGMENTS, i * 8, row, 0, 8);
                put(data, count++, row);
            }
        } else {
            for (int i = 0; i < frozenSegmentCount; i++) {
                System.arraycopy(FROZEN_SEGMENTS, i * 8, row, 0, 8);
                put(data, count++, row);
            }
        }
        upload(data);
        return count;
    }

    private static void put(FloatBuffer data, int index, float[] row) {
        for (int k = 0; k < 4; k++) {
            data.put(index * 4 + k, row[k]);
            data.put((MAX_LINES + index) * 4 + k, row[4 + k]);
        }
    }

    /** 经过 (px, py)、方向 (dx, dy) 的直线与屏幕矩形的两个交点（uv）。 */
    private static boolean clipLine(double px, double py, double dx, double dy, float[] out) {
        double lo = Double.NEGATIVE_INFINITY;
        double hi = Double.POSITIVE_INFINITY;
        if (Math.abs(dx) > 1.0E-9) {
            double a = -px / dx;
            double b = (1.0 - px) / dx;
            lo = Math.max(lo, Math.min(a, b));
            hi = Math.min(hi, Math.max(a, b));
        }
        if (Math.abs(dy) > 1.0E-9) {
            double a = -py / dy;
            double b = (1.0 - py) / dy;
            lo = Math.max(lo, Math.min(a, b));
            hi = Math.min(hi, Math.max(a, b));
        }
        if (!(hi > lo) || Double.isInfinite(lo) || Double.isInfinite(hi)) return false;
        out[0] = (float) (px + dx * lo);
        out[1] = (float) (py + dy * lo);
        out[2] = (float) (px + dx * hi);
        out[3] = (float) (py + dy * hi);
        return true;
    }

    /**
     * 把世界斩痕投影到屏幕（只取最新的 40 条），近裁剪面后面的部分裁掉。
     * 宽度按斩痕实际粗细和中点深度做透视：近处的斩痕错位带更宽、更强，远处的细而弱。
     */
    private static int projectWorld(double t, float[] out) {
        int count = 0;
        ZenithRiftClient.Slash[] slashes = rift.slashes;
        for (int i = slashes.length - 1; i >= 0 && count < MAX_WORLD_SEGMENTS; i--) {
            ZenithRiftClient.Slash slash = slashes[i];
            float light = slash.brightness(t);
            if (light <= 0.05F) continue;
            double length = slash.length() * slash.grownAt(t);
            if (length < 0.05) continue;
            CLIP_A.set((float) (slash.x() - camera.x), (float) (slash.y() - camera.y), (float) (slash.z() - camera.z), 1.0F);
            CLIP_B.set((float) (slash.x() + slash.dx() * length - camera.x), (float) (slash.y() + slash.dy() * length - camera.y),
                    (float) (slash.z() + slash.dz() * length - camera.z), 1.0F);
            VIEW.transform(CLIP_A);
            PROJECTION.transform(CLIP_A);
            VIEW.transform(CLIP_B);
            PROJECTION.transform(CLIP_B);
            if (CLIP_A.w < NEAR_W && CLIP_B.w < NEAR_W) continue;
            if (CLIP_A.w < NEAR_W) clipNear(CLIP_A, CLIP_B);
            else if (CLIP_B.w < NEAR_W) clipNear(CLIP_B, CLIP_A);
            float ax = CLIP_A.x / CLIP_A.w * 0.5F + 0.5F;
            float ay = CLIP_A.y / CLIP_A.w * 0.5F + 0.5F;
            float bx = CLIP_B.x / CLIP_B.w * 0.5F + 0.5F;
            float by = CLIP_B.y / CLIP_B.w * 0.5F + 0.5F;
            if (Math.max(ax, bx) < -0.1F || Math.min(ax, bx) > 1.1F || Math.max(ay, by) < -0.1F || Math.min(ay, by) > 1.1F) continue;
            if (Math.abs(bx - ax) + Math.abs(by - ay) < 0.004F) continue;
            float depth = Math.max(NEAR_W, (CLIP_A.w + CLIP_B.w) * 0.5F);
            float width = (float) (slash.width() * PROJECTION.m11() / (2.0 * depth));
            float strength = Math.min(1.0F, light);
            int o = count * 8;
            out[o] = ax;
            out[o + 1] = ay;
            out[o + 2] = bx;
            out[o + 3] = by;
            out[o + 4] = 1.0F;
            out[o + 5] = 0.12F * strength;
            out[o + 6] = Math.min(0.02F, Math.max(0.0015F, width * 1.5F)) * strength;
            out[o + 7] = Math.min(0.02F, Math.max(0.0006F, width * 0.5F));
            count++;
        }
        return count;
    }

    /** behind 在近裁剪面之后：沿齐次坐标插值到 w = NEAR_W。 */
    private static void clipNear(Vector4f behind, Vector4f front) {
        float k = (NEAR_W - behind.w) / (front.w - behind.w);
        behind.set(behind.x + (front.x - behind.x) * k, behind.y + (front.y - behind.y) * k,
                behind.z + (front.z - behind.z) * k, NEAR_W);
    }

    private static FloatBuffer lineData() {
        if (lineData == null) lineData = MemoryUtil.memAllocFloat(MAX_LINES * 2 * 4);
        return lineData;
    }

    private static void upload(FloatBuffer data) {
        if (lineTexture < 0) {
            lineTexture = GlStateManager._genTexture();
            GlStateManager._bindTexture(lineTexture);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        } else {
            GlStateManager._bindTexture(lineTexture);
        }
        // 原版上传贴图时会改这几项像素存储参数且不复原
        GlStateManager._pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GlStateManager._pixelStore(GL11.GL_UNPACK_ALIGNMENT, 4);
        data.clear();
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, MAX_LINES, 2, 0, GL11.GL_RGBA, GL11.GL_FLOAT, data);
        GlStateManager._bindTexture(0);
    }

    // ---------------------------------------------------------------- 画面处理

    private static void process(ShaderInstance shader, RenderTarget source, RenderTarget target, double t, int count,
            float aspect) {
        target.bindWrite(true);
        boolean frozenPhase = t >= ZenithRiftArts.FREEZE;
        double freezeIn = frozenPhase ? ZenithRiftClient.smooth((t - ZenithRiftArts.FREEZE) / 3.0) : 0.0;
        float darken = (float) (0.45 * ZenithRiftClient.smooth(t / ZenithRiftArts.SLASH_START) + 0.1 * freezeIn);
        float desaturate = (float) (0.18 * ZenithRiftClient.smooth((t - 4.0) / 8.0) + 0.4 * freezeIn);
        float cold = (float) (0.35 * ZenithRiftClient.smooth((t - 4.0) / 10.0) + 0.45 * freezeIn);
        float freeze = frozenPhase
                ? (float) (0.5 * freezeIn + 1.2 * ZenithRiftClient.smooth((t - (ZenithRiftArts.SHEATH - 2.0)) / 5.0))
                : 0.0F;
        shader.setSampler("DiffuseSampler", source.getColorTextureId());
        shader.setSampler("LineSampler", lineTexture);
        set(shader, "LineCount", count);
        set(shader, "Darken", darken);
        set(shader, "Desaturate", desaturate);
        set(shader, "Cold", cold);
        set(shader, "Freeze", freeze);
        set(shader, "Aspect", aspect);
        RenderSystem.disableBlend();
        fullscreen(shader);
    }

    private static void set(ShaderInstance shader, String name, float value) {
        shader.safeGetUniform(name).set(value);
    }

    private static void fullscreen(ShaderInstance shader) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(() -> shader);
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        builder.vertex(-1.0D, -1.0D, 0.0D).endVertex();
        builder.vertex(3.0D, -1.0D, 0.0D).endVertex();
        builder.vertex(-1.0D, 3.0D, 0.0D).endVertex();
        BufferUploader.drawWithShader(builder.end());
    }

    // ---------------------------------------------------------------- 碎裂

    /** 用 24 条切线依次切分屏幕矩形，得到凸多边形碎片（最多 301 块）。 */
    private static List<Shard> split(float aspect) {
        List<float[]> pieces = new ArrayList<>();
        pieces.add(new float[]{0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 1.0F});
        if (cutCount == 0) buildCuts(aspect);
        for (int i = 0; i < cutCount && pieces.size() < MAX_SHARDS; i++) {
            float px = CUT_X[i];
            float py = CUT_Y[i];
            float nx = -CUT_DY[i];
            float ny = CUT_DX[i];
            List<float[]> next = new ArrayList<>(pieces.size() * 2);
            for (float[] piece : pieces) {
                if (next.size() + 2 > MAX_SHARDS) {
                    next.add(piece);
                    continue;
                }
                float[] positive = clip(piece, px, py, nx, ny);
                float[] negative = clip(piece, px, py, -nx, -ny);
                if (positive != null) next.add(positive);
                if (negative != null) next.add(negative);
            }
            pieces = next;
        }
        Random random = new Random(rift.seed ^ 0x3D91B7E2A45C0F17L);
        List<Shard> out = new ArrayList<>(pieces.size());
        for (float[] piece : pieces) {
            int n = piece.length / 2;
            float[] xs = new float[n];
            float[] ys = new float[n];
            float cx = 0.0F;
            float cy = 0.0F;
            for (int k = 0; k < n; k++) {
                xs[k] = piece[k * 2];
                ys[k] = piece[k * 2 + 1];
                cx += xs[k];
                cy += ys[k];
            }
            cx /= n;
            cy /= n;
            double angle = Math.atan2(cy - 0.5, (cx - 0.5) * aspect) + (random.nextDouble() - 0.5) * 1.2;
            out.add(new Shard(xs, ys, cx, cy, (float) Math.cos(angle), (float) Math.sin(angle),
                    0.006F + random.nextFloat() * 0.014F, (random.nextFloat() - 0.5F) * 0.06F,
                    0.0025F * (0.7F + random.nextFloat() * 0.6F)));
        }
        return out;
    }

    /** 保留多边形在 (p, n) 正侧的部分；不足三个顶点时返回 null。 */
    private static float[] clip(float[] poly, float px, float py, float nx, float ny) {
        int n = poly.length / 2;
        float[] out = new float[(n + 1) * 2];
        int m = 0;
        for (int k = 0; k < n; k++) {
            float ax = poly[k * 2];
            float ay = poly[k * 2 + 1];
            float bx = poly[(k + 1) % n * 2];
            float by = poly[(k + 1) % n * 2 + 1];
            float da = (ax - px) * nx + (ay - py) * ny;
            float db = (bx - px) * nx + (by - py) * ny;
            if (da >= 0.0F) {
                out[m++] = ax;
                out[m++] = ay;
            }
            if (da >= 0.0F != db >= 0.0F) {
                float k2 = da / (da - db);
                out[m++] = ax + (bx - ax) * k2;
                out[m++] = ay + (by - ay) * k2;
            }
        }
        if (m < 6) return null;
        float[] trimmed = new float[m];
        System.arraycopy(out, 0, trimmed, 0, m);
        return area(trimmed) < 1.0E-7F ? null : trimmed;
    }

    private static float area(float[] poly) {
        int n = poly.length / 2;
        float sum = 0.0F;
        for (int k = 0; k < n; k++) {
            int j = (k + 1) % n;
            sum += poly[k * 2] * poly[j * 2 + 1] - poly[j * 2] * poly[k * 2 + 1];
        }
        return Math.abs(sum) * 0.5F;
    }

    /** 碎片直接画在实时画面上：飞开的地方露出的就是当前的世界。 */
    private static void shatter(RenderTarget main, double t, int width, int height) {
        main.bindWrite(true);
        float s = (float) (t - ZenithRiftArts.SHATTER);
        float shardAlpha = (float) (1.0 - ZenithRiftClient.smooth((t - (ZenithRiftArts.RELEASE - 8.0)) / 8.0));
        if (shardAlpha <= 0.003F || shards.isEmpty()) return;

        beginScreenSpace();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, processed.getColorTextureId());
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
            int alpha = Math.round(shardAlpha * 255.0F);
            float[] px = SHARD_X;
            float[] py = SHARD_Y;
            for (Shard shard : shards) {
                int n = transform(shard, s, width, height, px, py);
                for (int k = 1; k + 1 < n; k++) {
                    shardVertex(builder, px[0], py[0], shard.xs[0], shard.ys[0], width, height, alpha);
                    shardVertex(builder, px[k], py[k], shard.xs[k], shard.ys[k], width, height, alpha);
                    shardVertex(builder, px[k + 1], py[k + 1], shard.xs[k + 1], shard.ys[k + 1], width, height, alpha);
                }
            }
            BufferUploader.drawWithShader(builder.end());

            float glow = (float) ((0.25 + 0.9 * Math.exp(-s / 5.0)) * shardAlpha);
            float thickness = Math.max(1.0F, height / 720.0F);
            // 原版 position_color 着色器应用时会把混合方式改回普通透明混合（它的 JSON 自带 blend），
            // 叠加混合在这里不可靠，描边按透明度混合。
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            int edgeAlpha = Math.min(255, Math.round(glow * 255.0F));
            for (Shard shard : shards) {
                int n = transform(shard, s, width, height, px, py);
                for (int k = 0; k < n; k++) {
                    int j = (k + 1) % n;
                    edge(builder, px[k], py[k], px[j], py[j], thickness, width, height, edgeAlpha);
                }
            }
            BufferUploader.drawWithShader(builder.end());
        } finally {
            endScreenSpace();
        }
    }

    /** 碎片在第 s tick 的像素坐标：沿方向飞出、受重力坠落、绕中心旋转并略微缩小。 */
    private static int transform(Shard shard, float s, int width, int height, float[] px, float[] py) {
        int n = Math.min(shard.xs.length, px.length);
        float travel = 0.006F + shard.speed * s;
        float ox = shard.dirX * travel * height;
        float oy = (shard.dirY * travel - 0.5F * shard.gravity * s * s) * height;
        float angle = shard.spin * s;
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float scale = Math.max(0.5F, 0.985F - 0.005F * s);
        float cx = shard.cx * width;
        float cy = shard.cy * height;
        for (int k = 0; k < n; k++) {
            float rx = (shard.xs[k] * width - cx) * scale;
            float ry = (shard.ys[k] * height - cy) * scale;
            px[k] = cx + ox + rx * cos - ry * sin;
            py[k] = cy + oy + rx * sin + ry * cos;
        }
        return n;
    }

    private static void shardVertex(BufferBuilder builder, float x, float y, float u, float v, int width, int height,
            int alpha) {
        builder.vertex(x / width * 2.0F - 1.0F, y / height * 2.0F - 1.0F, 0.0F).uv(u, v)
                .color(255, 255, 255, alpha).endVertex();
    }

    private static void edge(BufferBuilder builder, float ax, float ay, float bx, float by, float thickness,
            int width, int height, int alpha) {
        float dx = bx - ax;
        float dy = by - ay;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5F) return;
        float nx = -dy / length * thickness;
        float ny = dx / length * thickness;
        edgeVertex(builder, ax + nx, ay + ny, width, height, alpha);
        edgeVertex(builder, bx + nx, by + ny, width, height, alpha);
        edgeVertex(builder, bx - nx, by - ny, width, height, alpha);
        edgeVertex(builder, ax - nx, ay - ny, width, height, alpha);
    }

    private static void edgeVertex(BufferBuilder builder, float x, float y, int width, int height, int alpha) {
        builder.vertex(x / width * 2.0F - 1.0F, y / height * 2.0F - 1.0F, 0.0F).color(150, 195, 255, alpha).endVertex();
    }

    // ---------------------------------------------------------------- 闪光

    /** 定格时一下轻闪，碎裂时一下强闪。强度乘以配置的闪光强度。 */
    private static void flash(double t) {
        double f = 0.0;
        if (t >= ZenithRiftArts.FREEZE) f += 0.3 * Math.exp(-(t - ZenithRiftArts.FREEZE) / 2.5);
        if (t >= ZenithRiftArts.SHATTER) f += 0.95 * Math.exp(-(t - ZenithRiftArts.SHATTER) / 3.0);
        f *= SenDimsConfig.ZENITH_FLASH_INTENSITY.get();
        if (f <= 0.003) return;
        int c = Math.min(255, (int) Math.round(f * 255.0));
        beginScreenSpace();
        try {
            // 白闪用白色加透明度做普通混合。不能用叠加混合配不透明的灰色：position_color 着色器应用时
            // 会把混合方式改回普通透明混合，不透明的灰色就成了盖住全屏、逐渐变黑的方块（之前的黑屏就是这个）。
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(-1.0F, -1.0F, 0.0F).color(255, 255, 255, c).endVertex();
            builder.vertex(1.0F, -1.0F, 0.0F).color(255, 255, 255, c).endVertex();
            builder.vertex(1.0F, 1.0F, 0.0F).color(255, 255, 255, c).endVertex();
            builder.vertex(-1.0F, 1.0F, 0.0F).color(255, 255, 255, c).endVertex();
            BufferUploader.drawWithShader(builder.end());
        } finally {
            endScreenSpace();
        }
    }

    // ---------------------------------------------------------------- GL 状态

    /** 投影和模型视图都设为单位矩阵，顶点直接给 NDC 坐标。 */
    private static void beginScreenSpace() {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void endScreenSpace() {
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void restoreState() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    private static TextureTarget ensure(TextureTarget target, int width, int height) {
        if (target == null) {
            target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
        } else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
            target.setFilterMode(GL11.GL_LINEAR);
        }
        return target;
    }

    private static void blit(RenderTarget source, RenderTarget target) {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
        boolean same = source.width == target.width && source.height == target.height;
        GlStateManager._glBlitFrameBuffer(0, 0, source.width, source.height, 0, 0, target.width, target.height,
                GL11.GL_COLOR_BUFFER_BIT, same ? GL11.GL_NEAREST : GL11.GL_LINEAR);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }
}
