package com.tonywww.slashblade_sendims.client.ritual;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.config.SenDimsConfig;
import com.tonywww.slashblade_sendims.network.RitualVisualPacket;
import com.tonywww.slashblade_sendims.network.RitualVisualPacket.Kind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 仪式特效的客户端：接收阶段包，维护进行中的仪式；每帧开头烘焙画布（自写着色器，世界绘制之外），
 * 在 AFTER_PARTICLES 用原版发光材质把画布贴到世界里的面片上（光影包会接管这些材质）。
 * 设计见整合包 docs/development/仪式着色器特效设计-2026-11-13.md。
 */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class RitualVisualClient {
    private static final int MAX_RITUALS = 8;
    private static final int SLOT_COUNT = 3;
    private static final long ARRIVAL_SALT = 0x5DEECE66DL;
    private static final double SKY_BEAM_RANGE = 160.0D;
    private static final double DETAIL_RANGE = 48.0D;
    private static final double RIPPLE_RANGE = 32.0D;
    private static final Map<String, Integer> STYLES = Map.of(
            "default", 0, "aether", 1, "undergarden", 2, "tofu", 3, "deep_realm_1", 4,
            "deep_realm_2", 5, "return", 6, "map_teleport", 7, "bedrock", 8);

    /** 能量流源头（相对核心方块，取方块顶面）：召唤为顶层十字 4 块和中层 4 角，传送为地板外圈 8 块。 */
    private static final int[][] SUMMON_SOURCES = {{1, 2, 0}, {-1, 2, 0}, {0, 2, 1}, {0, 2, -1}, {1, 1, 1}, {-1, 1, -1}, {1, 1, -1}, {-1, 1, 1}};
    private static final int[][] PORTAL_SOURCES = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 1}, {-1, 0, -1}, {1, 0, -1}, {-1, 0, 1}};

    private static final LinkedHashMap<Long, Ritual> RITUALS = new LinkedHashMap<>();
    private static Slot[] slots;
    private static MultiBufferSource.BufferSource buffers;
    private static Object world;
    private static int frame;

    private RitualVisualClient() {
    }

    enum Phase { CHARGE, RELEASE, INTERRUPT, ARRIVAL }

    private static final class Ritual {
        final long key;
        final Kind kind;
        final int style;
        final float[] colorA;
        final float[] colorB;
        final int colorAInt;
        final int coreX;
        final int coreY;
        final int coreZ;
        final double centerX;
        final double centerY;
        final double centerZ;
        double start;
        final int duration;
        final float heading;
        RitualGeometry.Decal decal;
        RitualGeometry.Decal shock;
        Phase phase;
        double phaseStart;
        double expireAt;
        float frozenProgress = 1.0F;
        float levelFrom;
        float levelTo;
        double levelAt;
        Vec3 departure;
        Slot slot;
        boolean baked;

        Ritual(long key, RitualVisualPacket packet, Phase phase, double now) {
            this.key = key;
            this.kind = packet.kind();
            this.style = STYLES.getOrDefault(packet.style().toLowerCase(Locale.ROOT), 0);
            this.colorA = rgb(packet.colorA());
            this.colorB = rgb(packet.colorB());
            this.colorAInt = packet.colorA();
            this.phase = phase;
            this.start = now;
            this.phaseStart = now;
            this.duration = Math.max(1, packet.duration());
            if (phase == Phase.ARRIVAL) {
                this.centerX = packet.x();
                this.centerY = packet.y();
                this.centerZ = packet.z();
                this.coreX = Mth.floor(packet.x());
                this.coreY = Mth.floor(packet.y());
                this.coreZ = Mth.floor(packet.z());
            } else {
                this.coreX = Mth.floor(packet.x());
                this.coreY = Mth.floor(packet.y());
                this.coreZ = Mth.floor(packet.z());
                this.centerX = coreX + 0.5;
                this.centerY = coreY;
                this.centerZ = coreZ + 0.5;
            }
            float param = packet.param();
            this.heading = kind == Kind.PORTAL && Float.isFinite(param) ? (float) Math.toRadians(param) : -100.0F;
        }

        float radius() {
            if (phase == Phase.ARRIVAL) return 2.5F;
            return switch (kind) {
                case SUMMON -> 5.0F;
                case PORTAL -> 3.5F;
                case BREACH -> 3.0F;
            };
        }

        float progress(double now) {
            if (phase == Phase.CHARGE && kind != Kind.BREACH) return (float) Mth.clamp((now - start) / duration, 0.0, 1.0);
            if (phase == Phase.INTERRUPT) return frozenProgress;
            return 1.0F;
        }

        float breachLevel(double now) {
            double k = Mth.clamp((now - levelAt) / 4.0, 0.0, 1.0);
            k = k * k * (3.0 - 2.0 * k);
            return (float) (levelFrom + (levelTo - levelFrom) * k);
        }

        boolean structured() {
            return phase != Phase.ARRIVAL && kind != Kind.BREACH;
        }

        /** 光柱底部：召唤在结构顶部（核心上方 2 格），传送在核心方块顶部，基岩在被敲方块顶部。 */
        double beamBaseY() {
            return switch (kind) {
                case SUMMON -> coreY + 2.0;
                case PORTAL -> coreY + 1.0;
                case BREACH -> coreY + 1.0;
            };
        }

        double distanceSqr(Vec3 camera) {
            double dx = centerX - camera.x;
            double dy = centerY - camera.y;
            double dz = centerZ - camera.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    /** 一套画布：法阵、冲击波、光幕、光柱、能量流。 */
    private static final class Slot {
        final RitualCanvas circle;
        final RitualCanvas shock;
        final RitualCanvas runes;
        final RitualCanvas beam;
        final RitualCanvas stream;
        Ritual owner;

        Slot(int index, int circleSize) {
            circle = new RitualCanvas("slot" + index + "_circle", circleSize, circleSize, false);
            shock = new RitualCanvas("slot" + index + "_shock", 512, 512, false);
            runes = new RitualCanvas("slot" + index + "_runes", 1024, 256, true);
            beam = new RitualCanvas("slot" + index + "_beam", 64, 512, false);
            stream = new RitualCanvas("slot" + index + "_stream", 256, 32, false);
        }
    }

    private static float[] rgb(int color) {
        return new float[]{(color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F};
    }

    private static boolean enabled() {
        return SenDimsConfig.RITUAL_ENABLED.get();
    }

    private static void world() {
        var level = Minecraft.getInstance().level;
        if (level != world) {
            world = level;
            for (Ritual ritual : RITUALS.values()) unassign(ritual);
            RITUALS.clear();
        }
    }

    private static void unassign(Ritual ritual) {
        if (ritual.slot != null && ritual.slot.owner == ritual) ritual.slot.owner = null;
        ritual.slot = null;
    }

    public static void accept(RitualVisualPacket packet) {
        world();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !enabled() || !level.dimension().location().equals(packet.dimension())) return;
        double now = level.getGameTime();
        switch (packet.stage()) {
            case CHARGE -> charge(level, packet, now);
            case INTERRUPT -> {
                Ritual ritual = RITUALS.get(packet.id());
                if (ritual != null && ritual.phase == Phase.CHARGE) {
                    ritual.frozenProgress = ritual.progress(now);
                    ritual.phase = Phase.INTERRUPT;
                    ritual.phaseStart = now;
                    ritual.expireAt = now + 16;
                }
            }
            case RELEASE -> release(level, packet, now);
            case WARP -> RitualScreenEffect.warp(packet.kind() == Kind.BREACH ? 1 : 0, packet.colorA(), packet.duration());
            case ARRIVAL -> {
                long key = packet.id() ^ ARRIVAL_SALT;
                Ritual ritual = new Ritual(key, packet, Phase.ARRIVAL, now);
                ritual.decal = RitualGeometry.sample(level, ritual.centerX, ritual.centerZ, Mth.floor(packet.y()),
                        ritual.radius(), ritual.radius() + 0.4F, RitualGeometry.Footprint.NONE);
                ritual.expireAt = now + 32;
                put(ritual);
                if (mc.player != null && packet.entity() == mc.player.getId()) {
                    RitualScreenEffect.arrive(packet.colorA(), packet.kind() == Kind.BREACH);
                }
            }
        }
    }

    private static void charge(ClientLevel level, RitualVisualPacket packet, double now) {
        Ritual existing = RITUALS.get(packet.id());
        if (packet.kind() == Kind.BREACH) {
            float target = Float.isFinite(packet.param()) ? Mth.clamp(packet.param() / 3.0F, 0.0F, 1.0F) : 0.34F;
            if (existing != null && existing.kind == Kind.BREACH && existing.phase == Phase.CHARGE) {
                existing.levelFrom = existing.breachLevel(now);
                existing.levelTo = target;
                existing.levelAt = now;
                existing.expireAt = now + packet.duration() + 10;
                return;
            }
            Ritual ritual = create(level, packet, now);
            ritual.levelFrom = 0.0F;
            ritual.levelTo = target;
            ritual.levelAt = now;
            ritual.expireAt = now + packet.duration() + 10;
            put(ritual);
            return;
        }
        Ritual ritual = create(level, packet, now);
        ritual.expireAt = now + packet.duration() + 40;
        put(ritual);
    }

    private static Ritual create(ClientLevel level, RitualVisualPacket packet, double now) {
        Ritual ritual = new Ritual(packet.id(), packet, Phase.CHARGE, now);
        RitualGeometry.Footprint footprint = switch (ritual.kind) {
            case SUMMON -> RitualGeometry.Footprint.THREE_BY_THREE;
            case PORTAL -> RitualGeometry.Footprint.CENTER;
            case BREACH -> RitualGeometry.Footprint.NONE;
        };
        ritual.decal = RitualGeometry.sample(level, ritual.centerX, ritual.centerZ, ritual.coreY,
                ritual.radius(), ritual.radius() + 0.4F, footprint);
        return ritual;
    }

    private static void release(ClientLevel level, RitualVisualPacket packet, double now) {
        Ritual ritual = RITUALS.get(packet.id());
        if (ritual == null || ritual.phase == Phase.INTERRUPT) {
            // 中途进入范围，或者服务端关闭了蓄能：直接从释放开始。
            ritual = create(level, packet, now);
            put(ritual);
        }
        ritual.phase = Phase.RELEASE;
        ritual.phaseStart = now;
        ritual.frozenProgress = 1.0F;
        switch (ritual.kind) {
            case SUMMON -> {
                ritual.shock = RitualGeometry.sample(level, ritual.centerX, ritual.centerZ, ritual.coreY,
                        12.0, 12.5F, RitualGeometry.Footprint.NONE);
                ritual.expireAt = now + 42;
            }
            case PORTAL -> {
                Entity traveler = packet.entity() >= 0 ? level.getEntity(packet.entity()) : null;
                ritual.departure = traveler != null ? traveler.position()
                        : new Vec3(ritual.centerX, ritual.coreY + 1.0, ritual.centerZ);
                ritual.expireAt = now + 26;
            }
            case BREACH -> {
                ritual.levelFrom = ritual.levelTo = 1.0F;
                ritual.shock = RitualGeometry.sample(level, ritual.centerX, ritual.centerZ, ritual.coreY,
                        8.0, 8.5F, RitualGeometry.Footprint.NONE);
                ritual.expireAt = now + 32;
            }
        }
        if (ritual.kind != Kind.PORTAL) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                Vec3 origin = new Vec3(ritual.centerX, ritual.beamBaseY(), ritual.centerZ);
                double distance = player.position().distanceTo(origin);
                if (distance < RIPPLE_RANGE) {
                    double k = 1.0 - distance / RIPPLE_RANGE;
                    RitualScreenEffect.ripple(origin, (float) (k * k), packet.colorA());
                }
            }
        }
    }

    private static void put(Ritual ritual) {
        Ritual previous = RITUALS.remove(ritual.key);
        if (previous != null) {
            ritual.slot = previous.slot;
            if (ritual.slot != null) ritual.slot.owner = ritual;
        }
        RITUALS.put(ritual.key, ritual);
        while (RITUALS.size() > MAX_RITUALS) {
            Iterator<Ritual> it = RITUALS.values().iterator();
            unassign(it.next());
            it.remove();
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        world();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || RITUALS.isEmpty()) return;
        double now = level.getGameTime();
        if (Minecraft.getInstance().screen instanceof ReceivingLevelScreen) {
            // 加载地形期间抵达符印不计时，画面关闭后玩家才看得到它
            for (Ritual ritual : RITUALS.values()) {
                if (ritual.phase != Phase.ARRIVAL) continue;
                ritual.start += 1;
                ritual.phaseStart += 1;
                ritual.expireAt += 1;
            }
        }
        Iterator<Ritual> it = RITUALS.values().iterator();
        while (it.hasNext()) {
            Ritual ritual = it.next();
            if (now >= ritual.expireAt) {
                if (ritual.phase == Phase.CHARGE && ritual.kind != Kind.BREACH) {
                    // 迟迟没有收到释放或中断：当作中断淡出。
                    ritual.frozenProgress = ritual.progress(now);
                    ritual.phase = Phase.INTERRUPT;
                    ritual.phaseStart = now;
                    ritual.expireAt = now + 16;
                    continue;
                }
                unassign(ritual);
                it.remove();
            }
        }
    }

    // ---------------------------------------------------------------- 画布烘焙（每帧开头，世界绘制之外）

    @SubscribeEvent
    public static void bake(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || RITUALS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !enabled() || !RitualShaders.bakeReady()) return;
        ensureSlots();
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        double now = level.getGameTime() + event.renderTickTime;
        float glow = IrisCompat.shaderPackInUse() ? SenDimsConfig.RITUAL_SHADER_PACK_GLOW.get().floatValue() : 1.0F;
        float intensity = SenDimsConfig.RITUAL_INTENSITY.get().floatValue();
        double maxDistance = SenDimsConfig.RITUAL_MAX_DISTANCE.get();
        frame++;
        boolean any = false;
        int index = 0;
        try {
            for (Ritual ritual : RITUALS.values()) {
                index++;
                double distance = Math.sqrt(ritual.distanceSqr(camera));
                boolean skyBeam = ritual.phase == Phase.RELEASE && ritual.kind != Kind.PORTAL;
                if (distance > (skyBeam ? Math.max(maxDistance, SKY_BEAM_RANGE) : maxDistance)) continue;
                if (!assign(ritual, distance, camera)) continue;
                if (ritual.baked && distance > DETAIL_RANGE && (frame + index) % 3 != 0) continue;
                bake(ritual, now, glow, intensity, distance <= DETAIL_RANGE ? 1.0F : 0.0F);
                ritual.baked = true;
                any = true;
            }
        } catch (RuntimeException error) {
            SenDims.LOGGER.warn("Ritual canvas bake failed; ritual shader visuals are disabled until resources reload", error);
            RitualShaders.circle = null;
        } finally {
            if (any) {
                RitualCanvas.restoreMainTarget();
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }
        }
    }

    private static void ensureSlots() {
        int circleSize = Integer.highestOneBit(Mth.clamp(SenDimsConfig.RITUAL_CANVAS_RESOLUTION.get(), 512, 2048));
        if (slots == null) {
            slots = new Slot[SLOT_COUNT];
            Map<RenderType, BufferBuilder> fixed = new HashMap<>();
            for (int i = 0; i < SLOT_COUNT; i++) {
                Slot slot = slots[i] = new Slot(i, circleSize);
                fixed.put(slot.circle.glow, new BufferBuilder(1 << 15));
                fixed.put(slot.shock.glow, new BufferBuilder(1 << 17));
                fixed.put(slot.runes.glow, new BufferBuilder(1 << 15));
                fixed.put(slot.beam.glow, new BufferBuilder(1 << 11));
                fixed.put(slot.stream.glow, new BufferBuilder(1 << 15));
            }
            buffers = MultiBufferSource.immediateWithBuffers(fixed, new BufferBuilder(256));
        }
        for (Slot slot : slots) slot.circle.resize(circleSize, circleSize);
    }

    /** 给仪式分配画布；没有空位时，比最远的占用者更近才抢过来。 */
    private static boolean assign(Ritual ritual, double distance, Vec3 camera) {
        if (ritual.slot != null && ritual.slot.owner == ritual) return true;
        ritual.slot = null;
        Slot farthest = null;
        double farthestDistance = distance;
        for (Slot slot : slots) {
            if (slot.owner == null || !RITUALS.containsValue(slot.owner)) {
                slot.owner = ritual;
                ritual.slot = slot;
                ritual.baked = false;
                return true;
            }
            double other = Math.sqrt(slot.owner.distanceSqr(camera));
            if (other > farthestDistance) {
                farthest = slot;
                farthestDistance = other;
            }
        }
        if (farthest == null) return false;
        farthest.owner.slot = null;
        farthest.owner.baked = false;
        farthest.owner = ritual;
        ritual.slot = farthest;
        ritual.baked = false;
        return true;
    }

    private static void bake(Ritual ritual, double now, float glow, float intensity, float detail) {
        Slot slot = ritual.slot;
        float time = (float) (now - ritual.start);
        float phaseTime = (float) (now - ritual.phaseStart);
        float progress = ritual.progress(now);
        float phase = switch (ritual.phase) {
            case CHARGE -> 0;
            case RELEASE -> 1;
            case INTERRUPT -> 2;
            case ARRIVAL -> 3;
        };
        float kind = ritual.kind.ordinal();
        float radius = ritual.radius();

        ShaderInstance circle = RitualShaders.circle;
        set(circle, "Time", time);
        set(circle, "Progress", progress);
        set(circle, "Phase", phase);
        set(circle, "PhaseTime", phaseTime);
        set(circle, "Mode", ritual.phase == Phase.ARRIVAL ? 2 : ritual.kind == Kind.BREACH ? 3 : 0);
        set(circle, "Kind", kind);
        set(circle, "Style", ritual.style);
        set(circle, "Radius", radius);
        set(circle, "Extent", radius + 0.4F);
        set(circle, "Heading", ritual.heading);
        set(circle, "Glow", glow);
        set(circle, "Intensity", intensity);
        set(circle, "Inner", ritual.kind == Kind.PORTAL && ritual.phase != Phase.ARRIVAL ? 1 : 0);
        set(circle, "Level", ritual.breachLevel(now));
        set(circle, "Detail", detail);
        color(circle, ritual);
        slot.circle.bake(circle);

        if (ritual.shock != null && ritual.phase == Phase.RELEASE) {
            set(circle, "Mode", 1);
            set(circle, "Radius", ritual.kind == Kind.BREACH ? 8.0F : 12.0F);
            set(circle, "Extent", ritual.shock.extent());
            slot.shock.bake(circle);
        }

        ShaderInstance beam = RitualShaders.beam;
        set(beam, "Time", time);
        set(beam, "Style", ritual.style);
        set(beam, "Glow", glow);
        set(beam, "Intensity", intensity);
        color(beam, ritual);
        set(beam, "Mode", 0);
        slot.beam.bake(beam);

        if (ritual.structured()) {
            if (ritual.phase == Phase.CHARGE) {
                set(beam, "Mode", 1);
                slot.stream.bake(beam);
            }
            ShaderInstance runes = RitualShaders.runes;
            set(runes, "Time", time);
            set(runes, "Progress", progress);
            set(runes, "Phase", phase);
            set(runes, "PhaseTime", phaseTime);
            set(runes, "Style", ritual.style);
            set(runes, "Glow", glow);
            set(runes, "Intensity", intensity);
            set(runes, "Detail", detail);
            color(runes, ritual);
            slot.runes.bake(runes);
        }
    }

    private static void set(ShaderInstance shader, String name, float value) {
        shader.safeGetUniform(name).set(value);
    }

    private static void color(ShaderInstance shader, Ritual ritual) {
        shader.safeGetUniform("ColorA").set(ritual.colorA[0], ritual.colorA[1], ritual.colorA[2]);
        shader.safeGetUniform("ColorB").set(ritual.colorB[0], ritual.colorB[1], ritual.colorB[2]);
    }

    // ---------------------------------------------------------------- 世界绘制（原版发光材质）

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        RitualScreenEffect.captureCamera(event.getProjectionMatrix(), event.getPoseStack().last().pose(),
                event.getCamera().getPosition());
        world();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || RITUALS.isEmpty() || slots == null || !enabled() || IrisCompat.shadowPass()) return;
        Vec3 camera = event.getCamera().getPosition();
        double now = level.getGameTime() + event.getPartialTick();
        PoseStack poses = event.getPoseStack();
        Matrix4f pose = poses.last().pose();
        Matrix3f normal = poses.last().normal();
        double maxDistance = SenDimsConfig.RITUAL_MAX_DISTANCE.get();
        boolean drew = false;
        for (Ritual ritual : RITUALS.values()) {
            if (ritual.slot == null || ritual.slot.owner != ritual || !ritual.baked) continue;
            double distance = Math.sqrt(ritual.distanceSqr(camera));
            boolean skyBeam = ritual.phase == Phase.RELEASE && ritual.kind != Kind.PORTAL;
            if (distance > (skyBeam ? Math.max(maxDistance, SKY_BEAM_RANGE) : maxDistance)) continue;
            double top = skyBeam ? 100.0 : 6.0;
            double r = ritual.shock != null ? 13.0 : ritual.radius() + 1.0;
            AABB bounds = new AABB(ritual.centerX - r, ritual.centerY - 4.0, ritual.centerZ - r,
                    ritual.centerX + r, ritual.centerY + top, ritual.centerZ + r);
            if (!event.getFrustum().isVisible(bounds)) continue;
            draw(ritual, now, pose, normal, camera);
            drew = true;
        }
        if (drew) {
            for (Slot slot : slots) {
                buffers.endBatch(slot.shock.glow);
                buffers.endBatch(slot.circle.glow);
                buffers.endBatch(slot.runes.glow);
                buffers.endBatch(slot.stream.glow);
                buffers.endBatch(slot.beam.glow);
            }
        }
    }

    private static double smooth(double k) {
        k = Mth.clamp(k, 0.0, 1.0);
        return k * k * (3.0 - 2.0 * k);
    }

    private static void draw(Ritual ritual, double now, Matrix4f pose, Matrix3f normal, Vec3 cam) {
        Slot slot = ritual.slot;
        double t = now - ritual.start;
        double pt = now - ritual.phaseStart;
        double cx = ritual.centerX;
        double cz = ritual.centerZ;
        double camX = cam.x;
        double camY = cam.y;
        double camZ = cam.z;

        RitualGeometry.decal(buffers.getBuffer(slot.circle.glow), pose, normal, ritual.decal, camX, camY, camZ, 1.0F);
        if (ritual.shock != null && ritual.phase == Phase.RELEASE) {
            RitualGeometry.decal(buffers.getBuffer(slot.shock.glow), pose, normal, ritual.shock, camX, camY, camZ, 1.0F);
        }

        VertexConsumer beam = buffers.getBuffer(slot.beam.glow);
        double baseY = ritual.beamBaseY();
        if (ritual.phase == Phase.ARRIVAL) {
            // 抵达：一道光柱从 6 格高落到地面
            double ground = ritual.decal.count() > 0 ? ritual.decal.maxY() : ritual.centerY;
            double fall = smooth(pt / 6.0);
            float k = (float) (1.0 - smooth((pt - 6.0) / 14.0));
            RitualGeometry.beam(beam, pose, normal, cx, ground + 6.0 * (1.0 - fall), cz,
                    2.2 + 1.8 * (1.0 - fall), 0.9, camX, camY, camZ, k);
            return;
        }
        if (ritual.kind == Kind.BREACH) {
            if (ritual.phase == Phase.RELEASE) {
                double width = 1.4 * (1.0 - smooth((pt - 3.0) / 17.0)) + 0.05;
                RitualGeometry.beam(beam, pose, normal, cx, baseY, cz, 24.0 * smooth(pt / 3.0), width,
                        camX, camY, camZ, 1.0F);
            }
            return;
        }

        double scale = ritual.kind == Kind.SUMMON ? 1.0 : 0.8;
        double wallRadius = ritual.kind == Kind.SUMMON ? 2.4 : 2.3;
        double wallMax = ritual.kind == Kind.SUMMON ? 3.5 : 2.8;
        double wallBase = ritual.coreY - 1.0;
        VertexConsumer runes = buffers.getBuffer(slot.runes.glow);
        switch (ritual.phase) {
            case CHARGE -> {
                double progress = ritual.progress(now);
                double stage = t < 13 ? 1 : t < 26 ? 2 : 3;
                double stageStart = t < 13 ? 0 : t < 26 ? 13 : 26;
                double height = (stage - 1 + smooth((t - stageStart) / 4.0)) * scale;
                double width = 0.9 * (t >= 30 ? 1.0 + 0.15 * Math.sin(t * 0.8) : 1.0);
                RitualGeometry.beam(beam, pose, normal, cx, baseY, cz, height, width, camX, camY, camZ, 1.0F);
                RitualGeometry.wall(runes, pose, normal, cx, wallBase, cz, wallRadius,
                        wallMax * smooth((progress - 0.2) / 0.7), camX, camY, camZ, 1.0F);
                streams(ritual, buffers.getBuffer(slot.stream.glow), pose, normal, cam, progress);
            }
            case RELEASE -> {
                if (ritual.kind == Kind.SUMMON) {
                    double width = 1.6 * (1.0 - smooth((pt - 4.0) / 26.0)) + 0.05;
                    RitualGeometry.beam(beam, pose, normal, cx, baseY, cz, 96.0 * smooth(pt / 3.0), width,
                            camX, camY, camZ, 1.0F);
                    RitualGeometry.wall(runes, pose, normal, cx, wallBase, cz, wallRadius,
                            wallMax * (1.0 + pt * 0.35), camX, camY, camZ, 1.0F);
                } else {
                    RitualGeometry.beam(beam, pose, normal, cx, baseY, cz, 3.0 * scale * (1.0 - smooth(pt / 6.0)),
                            0.9, camX, camY, camZ, 1.0F);
                    RitualGeometry.wall(runes, pose, normal, cx, wallBase, cz, wallRadius, wallMax,
                            camX, camY, camZ, 1.0F);
                    if (ritual.departure != null) {
                        Vec3 d = ritual.departure;
                        RitualGeometry.beam(beam, pose, normal, d.x, d.y, d.z, 2.4, 0.7 * (1.0 - smooth(pt / 8.0)),
                                camX, camY, camZ, 1.0F);
                    }
                }
            }
            case INTERRUPT -> {
                double height = 3.0 * scale * ritual.frozenProgress * (1.0 - smooth(pt / 6.0));
                RitualGeometry.beam(beam, pose, normal, cx, baseY, cz, height, 0.9, camX, camY, camZ, 1.0F);
                RitualGeometry.wall(runes, pose, normal, cx, wallBase, cz, wallRadius,
                        wallMax * smooth((ritual.frozenProgress - 0.2) / 0.7) * (1.0 - pt / 15.0),
                        camX, camY, camZ, 1.0F);
            }
            default -> {
            }
        }
    }

    /** 能量流：召唤从顶层十字 4 块和中层 4 角，传送从地板外圈 8 块，依次点亮，汇入光柱底部。 */
    private static void streams(Ritual ritual, VertexConsumer out, Matrix4f pose, Matrix3f normal, Vec3 cam,
            double progress) {
        double tx = ritual.centerX;
        double ty = ritual.beamBaseY() + 0.05;
        double tz = ritual.centerZ;
        int[][] offsets = ritual.kind == Kind.SUMMON ? SUMMON_SOURCES : PORTAL_SOURCES;
        for (int i = 0; i < offsets.length; i++) {
            double start = (i / 2) * 0.12;
            float k = (float) smooth((progress - start) / 0.1);
            if (k <= 0.0F) continue;
            int[] o = offsets[i];
            RitualGeometry.stream(out, pose, normal, ritual.coreX + o[0] + 0.5, ritual.coreY + o[1] + 0.02,
                    ritual.coreZ + o[2] + 0.5, tx, ty, tz, 0.9, 0.16, cam.x, cam.y, cam.z, k);
        }
    }
}
