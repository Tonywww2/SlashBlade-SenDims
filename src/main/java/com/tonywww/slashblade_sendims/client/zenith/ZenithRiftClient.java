package com.tonywww.slashblade_sendims.client.zenith;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.client.performance.IrisShadowPass;
import com.tonywww.slashblade_sendims.config.SenDimsConfig;
import com.tonywww.slashblade_sendims.network.ZenithRiftPacket;
import com.tonywww.slashblade_sendims.sa.ZenithRiftArts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * 真·次元斩的客户端：接收阶段包，按固定时间表播放世界层斩痕，并把屏幕层交给 {@link ZenithRiftScreen}。
 * <p>
 * 乱斩的斩击全部画在世界里：每条斩痕是绕自身轴线朝向镜头的细长面片，有透视、会被方块挡住，
 * 外面再套一层更宽更暗的光晕面片。用原版 {@link RenderType#eyes} 在 AFTER_PARTICLES 画（开光影时由 Iris 接管），
 * 光影的阴影阶段跳过。贴图在 CPU 上生成一次。时间按客户端 tick 计数，暂停时一起停。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class ZenithRiftClient {
    private static final int MAX_RIFTS = 4;
    private static final int SLASH_COUNT = 160;
    private static final ResourceLocation SLASH_TEXTURE = SenDims.prefix("zenith_rift/slash");

    private static final LinkedHashMap<Long, Rift> RIFTS = new LinkedHashMap<>();
    /** SE「裂空」的空间斩：几道交叉的斩痕，约半秒。与真·次元斩分开存放，数量多时不会挤掉进行中的 SA。 */
    private static final LinkedHashMap<Long, Rift> CUTS = new LinkedHashMap<>();
    private static final int MAX_CUTS = 24;
    private static final int CUT_LIFE = 9;
    private static MultiBufferSource.BufferSource buffers;
    private static RenderType slashType;
    private static boolean textureReady;
    private static boolean failed;

    private ZenithRiftClient() {
    }

    /** 一条斩痕：起点、方向、长度、宽度，出现的 tick 和存活时长。 */
    record Slash(double x, double y, double z, double dx, double dy, double dz, double length, double width,
            int spawn, int life) {
        /** 已划开的比例（0–1）。 */
        float grow(double t) {
            return (float) smooth((t - spawn) / 2.0);
        }

        /** 亮度：划开后保持，最后 2 tick 淡出；第 40 tick 时还在的斩痕定格到第 60 tick。 */
        float brightness(double t) {
            double age = t - spawn;
            if (age < 0) return 0.0F;
            if (t >= ZenithRiftArts.FREEZE) {
                if (t >= ZenithRiftArts.SHATTER || !frozen()) return 0.0F;
                age = ZenithRiftArts.FREEZE - spawn;
                return (float) Math.min(1.3, fade(age) * 1.25);
            }
            return (float) fade(age);
        }

        boolean frozen() {
            return spawn < ZenithRiftArts.FREEZE && spawn + life > ZenithRiftArts.FREEZE;
        }

        private double fade(double age) {
            return age >= life ? 0.0 : Math.min(1.0, (life - age) / 2.0);
        }

        double grownAt(double t) {
            return t >= ZenithRiftArts.FREEZE ? grow(ZenithRiftArts.FREEZE) : grow(t);
        }
    }

    static final class Rift {
        final long id;
        final ClientLevel level;
        final int caster;
        final Vec3 origin;
        final float radius;
        final long seed;
        final Slash[] slashes;
        final boolean cut;
        int age;
        boolean ended;

        Rift(ZenithRiftPacket packet, ClientLevel level) {
            this.cut = packet.stage() == ZenithRiftPacket.Stage.CUT;
            this.id = packet.id();
            this.level = level;
            this.caster = packet.caster();
            this.origin = new Vec3(packet.x(), packet.y(), packet.z());
            this.radius = packet.radius();
            this.seed = packet.seed();
            this.slashes = cut ? cutSlashes(origin, seed) : slashes(origin, radius, seed);
        }

        double time(float partialTick) {
            return age + partialTick;
        }

        boolean finished() {
            return ended || age >= (cut ? CUT_LIFE : ZenithRiftArts.DURATION);
        }

        /** 当前 tick 是否有斩痕要画。 */
        boolean drawing(double t) {
            return cut ? t < CUT_LIFE : t >= ZenithRiftArts.SLASH_START && t < ZenithRiftArts.SHATTER;
        }
    }

    /** 「裂空」：三道穿过中心附近、方向各异的斩痕，依次划开。 */
    private static Slash[] cutSlashes(Vec3 origin, long seed) {
        Random random = new Random(seed ^ 0x6A09E667F3BCC909L);
        Slash[] out = new Slash[3];
        double baseYaw = random.nextDouble() * Math.PI;
        for (int i = 0; i < out.length; i++) {
            double length = 4.5 + random.nextDouble() * 3.0;
            double yaw = baseYaw + i * Math.PI / 3.0 + (random.nextDouble() - 0.5) * 0.4;
            double pitch = (random.nextDouble() - 0.5) * Math.toRadians(100.0);
            double dx = Math.cos(yaw) * Math.cos(pitch);
            double dy = Math.sin(pitch);
            double dz = Math.sin(yaw) * Math.cos(pitch);
            double cx = origin.x + (random.nextDouble() - 0.5) * 0.8;
            double cy = origin.y + (random.nextDouble() - 0.5) * 0.8;
            double cz = origin.z + (random.nextDouble() - 0.5) * 0.8;
            out[i] = new Slash(cx - dx * length * 0.5, cy - dy * length * 0.5, cz - dz * length * 0.5, dx, dy, dz,
                    length, 0.10 + random.nextDouble() * 0.08, i == 2 ? 1 : 0, 5 + random.nextInt(2));
        }
        return out;
    }

    /** 所有观看者用同一个种子，看到的斩痕相同。 */
    private static Slash[] slashes(Vec3 origin, float radius, long seed) {
        Random random = new Random(seed ^ 0x2F6B1D7C93A5E48BL);
        Slash[] out = new Slash[SLASH_COUNT];
        int span = ZenithRiftArts.SLASH_END - ZenithRiftArts.SLASH_START;
        for (int i = 0; i < SLASH_COUNT; i++) {
            double r = radius * 0.85 * Math.sqrt(random.nextDouble());
            double a = random.nextDouble() * Math.PI * 2.0;
            double length = 14.0 + random.nextDouble() * 16.0;
            double yaw = random.nextDouble() * Math.PI * 2.0;
            double pitch = (random.nextDouble() - 0.5) * Math.toRadians(120.0);
            double dx = Math.cos(yaw) * Math.cos(pitch);
            double dy = Math.sin(pitch);
            double dz = Math.sin(yaw) * Math.cos(pitch);
            double cx = origin.x + Math.cos(a) * r;
            double cy = origin.y + 1.0 + (random.nextDouble() - 0.5) * 8.0;
            double cz = origin.z + Math.sin(a) * r;
            int spawn = ZenithRiftArts.SLASH_START + (int) ((long) i * span / SLASH_COUNT);
            int life = 4 + random.nextInt(5);
            out[i] = new Slash(cx - dx * length * 0.5, cy - dy * length * 0.5, cz - dz * length * 0.5, dx, dy, dz,
                    length, 0.08 + random.nextDouble() * 0.17, spawn, life);
        }
        return out;
    }

    public static void accept(ZenithRiftPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !level.dimension().location().equals(packet.dimension())) return;
        if (packet.stage() == ZenithRiftPacket.Stage.END) {
            Rift rift = RIFTS.get(packet.id());
            if (rift != null) {
                rift.ended = true;
                ZenithRiftScreen.stop(rift);
            }
            return;
        }
        if (!SenDimsConfig.ZENITH_ENABLED.get()) return;
        if (packet.stage() == ZenithRiftPacket.Stage.CUT) {
            if (CUTS.containsKey(packet.id())) return;
            while (CUTS.size() >= MAX_CUTS) {
                Iterator<Rift> it = CUTS.values().iterator();
                it.next();
                it.remove();
            }
            CUTS.put(packet.id(), new Rift(packet, level));
            return;
        }
        if (RIFTS.containsKey(packet.id())) return;
        while (RIFTS.size() >= MAX_RIFTS) {
            Iterator<Rift> it = RIFTS.values().iterator();
            ZenithRiftScreen.stop(it.next());
            it.remove();
        }
        Rift rift = new Rift(packet, level);
        RIFTS.put(rift.id, rift);
        if (SenDimsConfig.ZENITH_SCREEN_EFFECTS.get()) ZenithRiftScreen.start(rift);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || RIFTS.isEmpty() && CUTS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        CUTS.values().removeIf(cut -> ++cut.age >= CUT_LIFE || cut.level != mc.level);
        Iterator<Rift> it = RIFTS.values().iterator();
        while (it.hasNext()) {
            Rift rift = it.next();
            rift.age++;
            if (rift.finished() || rift.level != mc.level) {
                ZenithRiftScreen.stop(rift);
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RIFTS.clear();
        CUTS.clear();
        ZenithRiftScreen.stop(null);
    }

    /**
     * 施放者被定身（移动速度属性归零）时，原版会按移动速度把视野缩到一半。施放期间施放者的视野保持不变。
     */
    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        if (RIFTS.isEmpty()) return;
        int self = event.getPlayer().getId();
        for (Rift rift : RIFTS.values()) {
            if (!rift.ended && rift.caster == self && rift.age < ZenithRiftArts.RELEASE + 2) {
                event.setNewFovModifier(1.0F);
                return;
            }
        }
    }

    static Iterable<Rift> rifts() {
        return RIFTS.values();
    }

    // ---------------------------------------------------------------- 世界层

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || IrisShadowPass.isActive()) return;
        ZenithRiftScreen.captureCamera(event.getProjectionMatrix(), event.getPoseStack().last().pose(),
                event.getCamera().getPosition());
        if (RIFTS.isEmpty() && CUTS.isEmpty() || !SenDimsConfig.ZENITH_ENABLED.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !ensureResources()) return;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        Matrix4f pose = poses.last().pose();
        Matrix3f normal = poses.last().normal();
        float partialTick = event.getPartialTick();
        Slashes slashes = new Slashes(event, pose, normal, camera, partialTick);
        for (Rift rift : RIFTS.values()) slashes.draw(rift);
        for (Rift rift : CUTS.values()) slashes.draw(rift);
        VertexConsumer out = slashes.out;
        if (out != null) buffers.endBatch(slashType);
    }

    /** 一帧的斩痕绘制：真·次元斩与「裂空」共用。 */
    private static final class Slashes {
        final RenderLevelStageEvent event;
        final Matrix4f pose;
        final Matrix3f normal;
        final Vec3 camera;
        final float partialTick;
        VertexConsumer out;

        Slashes(RenderLevelStageEvent event, Matrix4f pose, Matrix3f normal, Vec3 camera, float partialTick) {
            this.event = event;
            this.pose = pose;
            this.normal = normal;
            this.camera = camera;
            this.partialTick = partialTick;
        }

        void draw(Rift rift) {
            if (rift.ended || rift.level != Minecraft.getInstance().level) return;
            double t = rift.time(partialTick);
            if (!rift.drawing(t)) return;
            double r = rift.radius + 8.0;
            AABB bounds = new AABB(rift.origin.x - r, rift.origin.y - 12.0, rift.origin.z - r,
                    rift.origin.x + r, rift.origin.y + 14.0, rift.origin.z + r);
            if (!event.getFrustum().isVisible(bounds)) return;
            if (out == null) out = buffers.getBuffer(slashType);
            for (Slash slash : rift.slashes) {
                float light = slash.brightness(t);
                if (light <= 0.01F) continue;
                float grow = (float) slash.grownAt(t);
                ribbon(out, pose, normal, slash, grow, light * 0.3F, 3.2, camera);
                ribbon(out, pose, normal, slash, grow, light, 1.0, camera);
            }
        }
    }

    /** 绕斩痕轴线朝向镜头的面片，正反两面都画（原版发光材质开着背面剔除）。 */
    private static void ribbon(VertexConsumer out, Matrix4f pose, Matrix3f normal, Slash slash, float grow,
            float light, double widthScale, Vec3 camera) {
        double length = slash.length * grow;
        if (length < 0.05) return;
        double ax = slash.x - camera.x;
        double ay = slash.y - camera.y;
        double az = slash.z - camera.z;
        double bx = ax + slash.dx * length;
        double by = ay + slash.dy * length;
        double bz = az + slash.dz * length;
        double mx = (ax + bx) * 0.5;
        double my = (ay + by) * 0.5;
        double mz = (az + bz) * 0.5;
        // side = dir × view
        double sx = slash.dy * mz - slash.dz * my;
        double sy = slash.dz * mx - slash.dx * mz;
        double sz = slash.dx * my - slash.dy * mx;
        double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (sl < 1.0E-6) return;
        double half = slash.width * widthScale * (0.6 + 0.4 * Math.min(1.0F, light)) * 0.5 / sl;
        sx *= half;
        sy *= half;
        sz *= half;
        float nx = (float) -mx;
        float ny = (float) -my;
        float nz = (float) -mz;
        float u1 = grow;
        int c = Mth.clamp((int) (light * 255.0F), 0, 255);
        vertex(out, pose, normal, ax + sx, ay + sy, az + sz, 0.0F, 0.0F, c, nx, ny, nz);
        vertex(out, pose, normal, bx + sx, by + sy, bz + sz, u1, 0.0F, c, nx, ny, nz);
        vertex(out, pose, normal, bx - sx, by - sy, bz - sz, u1, 1.0F, c, nx, ny, nz);
        vertex(out, pose, normal, ax - sx, ay - sy, az - sz, 0.0F, 1.0F, c, nx, ny, nz);
        vertex(out, pose, normal, ax - sx, ay - sy, az - sz, 0.0F, 1.0F, c, nx, ny, nz);
        vertex(out, pose, normal, bx - sx, by - sy, bz - sz, u1, 1.0F, c, nx, ny, nz);
        vertex(out, pose, normal, bx + sx, by + sy, bz + sz, u1, 0.0F, c, nx, ny, nz);
        vertex(out, pose, normal, ax + sx, ay + sy, az + sz, 0.0F, 0.0F, c, nx, ny, nz);
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, Matrix3f normal, double x, double y, double z,
            float u, float v, int c, float nx, float ny, float nz) {
        out.vertex(pose, (float) x, (float) y, (float) z).color(c, c, c, c).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(normal, nx, ny, nz).endVertex();
    }

    /** 斩痕贴图：沿长度方向（u）两端收尖，横向（v）亮白的芯加冷色的边，带细噪声。颜色已预乘。 */
    private static boolean ensureResources() {
        if (textureReady) return true;
        if (failed) return false;
        try {
            int w = 128;
            int h = 32;
            NativeImage image = new NativeImage(w, h, false);
            Random random = new Random(0x51A5L);
            for (int x = 0; x < w; x++) {
                double u = (x + 0.5) / w;
                double taper = smooth(u / 0.1) * smooth((1.0 - u) / 0.04);
                double grain = 0.85 + 0.15 * random.nextDouble();
                for (int y = 0; y < h; y++) {
                    double v = ((y + 0.5) / h - 0.5) * 2.0;
                    double core = Math.exp(-v * v / 0.012);
                    double edge = Math.exp(-v * v / 0.18) * 0.75;
                    double k = taper * grain;
                    int r = clamp255((core * 1.0 + edge * 0.35) * k);
                    int g = clamp255((core * 1.0 + edge * 0.55) * k);
                    int b = clamp255((core * 1.0 + edge * 1.0) * k);
                    int a = clamp255(Math.max(core, edge) * k);
                    image.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);
                }
            }
            Minecraft.getInstance().getTextureManager().register(SLASH_TEXTURE, new DynamicTexture(image));
            slashType = RenderType.eyes(SLASH_TEXTURE);
            buffers = MultiBufferSource.immediateWithBuffers(Map.of(slashType, new BufferBuilder(1 << 16)),
                    new BufferBuilder(256));
            textureReady = true;
        } catch (RuntimeException error) {
            failed = true;
            SenDims.LOGGER.warn("True Judgement Cut slash texture failed; world slashes are disabled for this session", error);
            return false;
        }
        return true;
    }

    private static int clamp255(double value) {
        return Mth.clamp((int) Math.round(value * 255.0), 0, 255);
    }

    static double smooth(double k) {
        k = Mth.clamp(k, 0.0, 1.0);
        return k * k * (3.0 - 2.0 * k);
    }
}
