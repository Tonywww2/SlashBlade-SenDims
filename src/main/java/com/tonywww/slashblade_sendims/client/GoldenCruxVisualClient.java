package com.tonywww.slashblade_sendims.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.network.GoldenCruxVisualPacket;
import com.tonywww.slashblade_sendims.network.GoldenCruxVisualPacket.Stage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class GoldenCruxVisualClient {
    private record PlantedSpike(Vec3 root, GoldenCruxGeometry.Spike template) {}
    private record Impact(Vec3 origin, List<PlantedSpike> spikes, List<GoldenCruxGeometry.Face> ring, AABB bounds) {}
    private record Visual(GoldenCruxVisualPacket packet, double began, Impact impact) {}
    private static final LinkedHashMap<Integer, Visual> FLIGHTS = new LinkedHashMap<>();
    private static final ArrayDeque<Visual> IMPACTS = new ArrayDeque<>();
    private static final ResourceLocation TEXTURE = SenDims.prefix("textures/effect/holy_sword_light.png");
    private static final RenderType BODY = RenderType.entityTranslucent(TEXTURE);
    private static final RenderType GLOW = RenderType.entityTranslucentEmissive(TEXTURE, false);
    // Fixed material buffers keep the whole effect at two draw calls, even with interleaved faces.
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediateWithBuffers(
            Map.of(BODY, new BufferBuilder(65536), GLOW, new BufferBuilder(65536)), new BufferBuilder(256));
    private static Object world;

    private GoldenCruxVisualClient() {}

    private static void world() {
        var level = Minecraft.getInstance().level;
        if (level != world) { world = level; FLIGHTS.clear(); IMPACTS.clear(); }
    }

    public static void accept(GoldenCruxVisualPacket packet) {
        world();
        var level = Minecraft.getInstance().level;
        if (level == null || !packet.valid() || !level.dimension().location().equals(packet.dimension())) return;
        Impact impact = packet.stage() == Stage.STAB || packet.stage() == Stage.BURST ? plant(level, packet) : null;
        Visual visual = new Visual(packet, level.getGameTime(), impact);
        if (packet.stage() == Stage.LEAP || packet.stage() == Stage.DIVE) {
            FLIGHTS.put(packet.entityId(), visual);
            while (FLIGHTS.size() > 32) FLIGHTS.remove(FLIGHTS.keySet().iterator().next());
        } else {
            Visual old = FLIGHTS.get(packet.entityId());
            if (old != null && old.packet.castId() == packet.castId()) FLIGHTS.remove(packet.entityId());
            if (impact != null) {
                IMPACTS.addLast(visual);
                while (IMPACTS.size() > 24) IMPACTS.removeFirst();
            }
        }
    }

    /** Bounded, one-time collision queries. Failed samples are cached as well. */
    private static final class GroundSampler {
        final ClientLevel level;
        final Entity user;
        final Vec3 origin;
        final Map<Vec3, Vec3> points = new HashMap<>();

        GroundSampler(ClientLevel level, GoldenCruxVisualPacket packet) {
            this.level = level;
            user = level.getEntity(packet.entityId());
            origin = packet.origin();
        }

        Vec3 sample(Vec3 offset) {
            Vec3 key = new Vec3(offset.x, 0, offset.z);
            if (points.containsKey(key)) return points.get(key);
            Vec3 requested = origin.add(key), result = null;
            if (level.hasChunkAt(BlockPos.containing(requested))) {
                // Start below a normal two-block ceiling. A wall/ceiling that
                // contains the ray start is not a valid upward-facing floor.
                var hit = level.clip(new ClipContext(requested.add(0, 1.5, 0), requested.add(0, -4, 0),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
                if (hit.getType() == HitResult.Type.BLOCK && hit.getDirection() == Direction.UP && !hit.isInside()) {
                    result = hit.getLocation();
                }
            }
            // No fallback to the player's Y: cliff edges and empty space stay empty.
            points.put(key, result);
            return result;
        }
    }

    private static Impact plant(ClientLevel level, GoldenCruxVisualPacket packet) {
        GroundSampler ground = new GroundSampler(level, packet);
        if (packet.stage() == Stage.STAB) {
            Vec3 floor = ground.sample(Vec3.ZERO);
            return floor == null ? null : new Impact(floor, List.of(), List.of(), new AABB(floor, floor).inflate(2.5));
        }
        Vec3 origin = packet.origin();
        List<PlantedSpike> spikes = new ArrayList<>(GoldenCruxGeometry.CROWN.size());
        double minimumY = origin.y, maximumY = origin.y;
        for (var template : GoldenCruxGeometry.CROWN) {
            Vec3 floor = ground.sample(template.anchor());
            if (floor == null) continue;
            spikes.add(new PlantedSpike(floor, template));
            minimumY = Math.min(minimumY, floor.y);
            maximumY = Math.max(maximumY, floor.y);
        }
        List<GoldenCruxGeometry.Face> ring = new ArrayList<>(GoldenCruxGeometry.RING.size());
        for (var face : GoldenCruxGeometry.RING) {
            Vec3 a = ground.sample(face.a()), b = ground.sample(face.b());
            Vec3 c = ground.sample(face.c()), d = ground.sample(face.d());
            Vec3 middle = ground.sample(face.a().add(face.b()).add(face.c()).add(face.d()).scale(0.25));
            if (a == null || b == null || c == null || d == null || middle == null) continue;
            double low = Math.min(middle.y, Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)));
            double high = Math.max(middle.y, Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)));
            // Split at steps and gaps rather than stretching a glowing bridge over them.
            if (high - low > 0.125) continue;
            minimumY = Math.min(minimumY, low);
            maximumY = Math.max(maximumY, high);
            a = a.subtract(origin).add(0, 0.025, 0);
            b = b.subtract(origin).add(0, 0.025, 0);
            c = c.subtract(origin).add(0, 0.025, 0);
            d = d.subtract(origin).add(0, 0.025, 0);
            ring.add(new GoldenCruxGeometry.Face(a, b, c, d, b.subtract(a).cross(c.subtract(a)).normalize(),
                    face.color(), face.alpha(), face.glow()));
        }
        if (spikes.isEmpty() && ring.isEmpty()) return null;
        AABB bounds = new AABB(origin.x - 4.5, minimumY - 0.2, origin.z - 4.5,
                origin.x + 4.5, maximumY + 4, origin.z + 4.5);
        return new Impact(origin, List.copyOf(spikes), List.copyOf(ring), bounds);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        world();
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        double now = level.getGameTime();
        FLIGHTS.values().removeIf(value -> now - value.began > 40);
        IMPACTS.removeIf(value -> now - value.began >= (value.packet.stage() == Stage.STAB ? 4 : 22));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        world();
        var mc = Minecraft.getInstance();
        if (mc.level == null || FLIGHTS.isEmpty() && IMPACTS.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        double now = mc.level.getGameTime() + event.getPartialTick();
        PoseStack poses = event.getPoseStack();
        for (Visual visual : FLIGHTS.values()) {
            var user = mc.level.getEntity(visual.packet.entityId());
            if (user == null) continue;
            Vec3 origin = user.getPosition(event.getPartialTick());
            if (origin.distanceToSqr(camera) > 96 * 96) continue;
            if (!event.getFrustum().isVisible(user.getBoundingBox().inflate(2))) continue;
            if (visual.packet.stage() == Stage.DIVE) {
                draw(GoldenCruxGeometry.SPEAR, poses, origin.subtract(camera).add(0, 0.2, 0), 1, 1, 0.85F);
            } else {
                float fade = (float) clamp(1 - (now - visual.began) / 12);
                draw(GoldenCruxGeometry.LEAP, poses, origin.subtract(camera), 1, 1, fade);
            }
        }
        for (Visual visual : IMPACTS) {
            Impact impact = visual.impact;
            Vec3 origin = impact.origin;
            double distance = origin.distanceToSqr(camera);
            if (distance > 96 * 96 || !event.getFrustum().isVisible(impact.bounds)) continue;
            float age = (float) (now - visual.began);
            if (visual.packet.stage() == Stage.STAB) {
                draw(GoldenCruxGeometry.SPEAR, poses, origin.subtract(camera).add(0, 1.48, 0), 1, 1, (float) clamp(1 - age / 4));
                continue;
            }
            float fade = (float) clamp((22 - age) / 8);
            double growth = clamp(age / 3.0);
            for (PlantedSpike spike : impact.spikes) {
                if (distance >= 48 * 48 && !spike.template.distant()) continue;
                draw(spike.template.mesh(), poses, spike.root.subtract(camera), 1, growth, fade);
            }
            // Grounded marks fade in place; scaling X/Z would move their sampled
            // vertices off the supporting block and reintroduce floating rings.
            draw(impact.ring, poses, origin.subtract(camera), 1,
                    1, fade * (float) (clamp(age / 3) * clamp(1 - age / 14)));
        }
        BUFFERS.endBatch(BODY);
        BUFFERS.endBatch(GLOW);
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }

    private static void draw(List<GoldenCruxGeometry.Face> mesh, PoseStack poses, Vec3 origin,
            double scale, double height, float fade) {
        if (mesh.isEmpty() || fade <= 0.005F || height <= 0.001) return;
        poses.pushPose();
        poses.translate(origin.x, origin.y, origin.z);
        poses.scale((float) scale, (float) height, (float) scale);
        PoseStack.Pose pose = poses.last();
        for (var face : mesh) {
            VertexConsumer consumer = BUFFERS.getBuffer(face.glow() ? GLOW : BODY);
            vertex(consumer, pose, face.a(), face.normal(), face.color(), face.alpha() * fade, 0, 0);
            vertex(consumer, pose, face.b(), face.normal(), face.color(), face.alpha() * fade, 1, 0);
            vertex(consumer, pose, face.c(), face.normal(), face.color(), face.alpha() * fade, 1, 1);
            vertex(consumer, pose, face.d(), face.normal(), face.color(), face.alpha() * fade, 0, 1);
        }
        poses.popPose();
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 position, Vec3 normal,
            int color, float alpha, float u, float v) {
        buffer.vertex(pose.pose(), (float) position.x, (float) position.y, (float) position.z)
                .color((color >> 16) & 255, (color >> 8) & 255, color & 255, (int) (clamp(alpha) * 255))
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }
}
