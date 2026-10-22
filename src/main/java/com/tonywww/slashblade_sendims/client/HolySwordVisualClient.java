package com.tonywww.slashblade_sendims.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tonywww.slashblade_sendims.SenDims;
import com.tonywww.slashblade_sendims.client.HolySwordGeometry.Kind;
import com.tonywww.slashblade_sendims.config.HolySwordVisualConfig;
import com.tonywww.slashblade_sendims.network.HolySwordVisualPacket;
import com.tonywww.slashblade_sendims.network.HolySwordVisualPacket.Stage;
import com.tonywww.slashblade_sendims.sa.HolySwordArts;
import com.tonywww.slashblade_sendims.se.HolySwordEffects;
import com.tonywww.slashblade_sendims.se.NamelessSeries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** World-space client effects, independent of the tiny vanilla entity render-distance heuristic. */
@Mod.EventBusSubscriber(modid = SenDims.MOD_ID, value = Dist.CLIENT)
public final class HolySwordVisualClient {
    // Vanilla emissive shader: LEQUAL depth, color-only writes, no culling, no full-screen passes.
    // Oculus may replace this shader with its entity shader, which is the intended fallback.
    private static final RenderType MATERIAL = RenderType.entityTranslucentEmissive(
            SenDims.prefix("textures/effect/holy_sword_atlas.png"), false);
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(262144));
    private record Key(UUID owner, long id, Kind kind) {}
    private static final Map<Key, Visual> CASTS = new LinkedHashMap<>();
    private static final Map<UUID, Visual> CHARGES = new LinkedHashMap<>();
    private static ClientLevel world;

    private static final class Visual {
        HolySwordVisualPacket packet;
        final Kind kind;
        double began, refreshed, pulse;
        double canceled = -1;
        int phase;
        float height;
        Visual(HolySwordVisualPacket p, Kind kind, double now, int offset) {
            packet = p; this.kind = kind; began = now - offset; refreshed = now; pulse = now;
            phase = p.stage() == Stage.PULSE_2 ? 2 : p.stage() == Stage.PULSE_1 ? 1 : 0;
            height = p.skyHeight();
        }
    }

    private static void checkWorld() {
        var current = Minecraft.getInstance().level;
        if (current != world) { CASTS.clear(); CHARGES.clear(); world = current; }
    }

    public static void accept(HolySwordVisualPacket p) {
        checkWorld();
        if (world == null || !world.dimension().location().equals(p.dimension()) || !p.valid()
                || !HolySwordVisualConfig.ENABLED.get()) return;
        double now = world.getGameTime();
        if (p.stage() == Stage.STOP_CHARGE) {
            Visual charge = CHARGES.get(p.user());
            if (charge != null && charge.packet.castId() == p.castId()) CHARGES.remove(p.user());
            return;
        }
        if (p.stage() == Stage.CANCEL) {
            CASTS.forEach((key, v) -> { if (key.owner.equals(p.user()) && key.id == p.castId()) v.canceled = now; });
            return;
        }
        if (p.stage() == Stage.CHARGE) {
            Visual charge = CHARGES.get(p.user());
            if (charge == null || charge.packet.castId() != p.castId()) {
                charge = new Visual(p, Kind.CHARGE, now, p.chargeTicks()); CHARGES.put(p.user(), charge);
            }
            charge.packet = p; charge.refreshed = now; charge.began = now - p.chargeTicks();
            trim(CHARGES, 32);
            return;
        }
        CHARGES.remove(p.user());
        Kind kind = p.excalibur() ? Kind.EXCALIBUR : p.stage() == Stage.OPEN ? Kind.OPEN : Kind.CALIBURN;
        if (kind == Kind.CALIBURN) CASTS.remove(new Key(p.user(), p.castId(), Kind.OPEN));
        Key key = new Key(p.user(), p.castId(), kind);
        Visual visual = CASTS.get(key);
        int phase = p.stage() == Stage.PULSE_2 ? 2 : p.stage() == Stage.PULSE_1 ? 1 : 0;
        if (visual == null) {
            visual = new Visual(p, kind, now, phase == 2 ? 10 : phase == 1 ? 4 : 0);
            CASTS.put(key, visual);
        } else if (phase > visual.phase) {
            visual.packet = p; visual.phase = phase; visual.pulse = now;
            visual.height = p.skyHeight();
        }
        trim(CASTS, 32);
    }
    private static <K> void trim(Map<K, Visual> map, int limit) {
        while (map.size() > limit) map.remove(map.keySet().iterator().next());
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        checkWorld();
        if (world == null) return;
        double now = world.getGameTime();
        CASTS.values().removeIf(v -> now - v.began > HolySwordGeometry.lifetime(v.kind)
                || v.canceled >= 0 && now - v.canceled > 4);
        CHARGES.values().removeIf(v -> now - v.refreshed > 24 || stoppedUsing(v, now));
        if (!HolySwordVisualConfig.ENABLED.get()) { CASTS.clear(); CHARGES.clear(); }
    }
    private static boolean stoppedUsing(Visual v, double now) {
        var entity = world.getEntity(v.packet.entityId());
        if (!(entity instanceof LivingEntity user) || !user.getUUID().equals(v.packet.user())) return false;
        var state = NamelessSeries.getBladeState(user);
        return now - v.refreshed > 2 && (!HolySwordEffects.charging(user, state)
                || !HolySwordArts.isArt(state.getSlashArtsKey()));
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        checkWorld();
        if (world == null || !HolySwordVisualConfig.ENABLED.get() || CASTS.isEmpty() && CHARGES.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        double now = world.getGameTime() + event.getPartialTick();
        ArrayList<Visual> effects = new ArrayList<>(CASTS.values()); effects.addAll(CHARGES.values());
        effects.sort(Comparator.comparingDouble(v -> v.packet.origin().distanceToSqr(camera)));
        var consumer = BUFFERS.getBuffer(MATERIAL);
        PoseStack.Pose pose = event.getPoseStack().last();
        HolySwordGeometry.Sink sink = (a, b, c, d, color, alpha, sprite) -> {
            if (alpha <= 0.005F) return;
            float u = sprite / 3.0F;
            vertex(consumer, pose, a.subtract(camera), color, alpha, u, 0);
            vertex(consumer, pose, b.subtract(camera), color, alpha, u + 1.0F / 3, 0);
            vertex(consumer, pose, c.subtract(camera), color, alpha, u + 1.0F / 3, 1);
            vertex(consumer, pose, d.subtract(camera), color, alpha, u, 1);
        };
        int budget = HolySwordVisualConfig.MOTE_BUDGET.get();
        for (Visual v : effects) {
            var p = v.packet;
            Vec3 origin = p.origin(), end = p.end();
            boolean firstPerson = mc.player != null && mc.player.getUUID().equals(p.user())
                    && mc.options.getCameraType().isFirstPerson();
            if (v.kind == Kind.CHARGE && world.getEntity(p.entityId()) instanceof LivingEntity user
                    && user.getUUID().equals(p.user())) {
                origin = user.getEyePosition(event.getPartialTick()).add(0, -0.25, 0);
                // Move the charging light out of the first-person camera's near plane.
                origin = origin.add(user.getLookAngle().scale(firstPerson ? 0.55 : 0));
                end = origin.add(user.getLookAngle().scale(1.2));
            }
            double distance = origin.distanceTo(camera), range = p.excalibur() ? 192 : 128;
            if (distance > range) continue;
            AABB bounds = new AABB(origin, end).inflate(p.excalibur() ? 25 : 17);
            bounds = bounds.minmax(new AABB(origin, origin.add(0, v.height, 0)).inflate(5));
            if (!event.getFrustum().isVisible(bounds)) continue;
            float age = (float) Math.max(0, now - v.began);
            float intensity = HolySwordVisualConfig.INTENSITY.get().floatValue();
            intensity *= (float) Mth.clamp((range - distance) / 16, 0, 1);
            if (v.canceled >= 0) intensity *= (float) Mth.clamp(1 - (now - v.canceled) / 4, 0, 1);
            if (v.kind != Kind.CHARGE && age >= HolySwordGeometry.lifetime(v.kind)) continue;
            boolean near = distance < HolySwordVisualConfig.DETAIL_DISTANCE.get();
            budget -= HolySwordGeometry.draw(new HolySwordGeometry.Frame(v.kind, origin, end, v.height,
                    age, (float) Math.max(0, now - v.pulse), v.phase,
                    p.castId() ^ p.user().getLeastSignificantBits(), near, firstPerson,
                    HolySwordVisualConfig.SKY_LIGHT.get(), intensity, budget), p.excalibur(), camera, sink);
        }
        // Flush only our material, never another mod's buffered geometry.
        BUFFERS.endBatch(MATERIAL);
    }
    private static void vertex(VertexConsumer b, PoseStack.Pose pose, Vec3 p, int rgb,
                               float alpha, float u, float v) {
        b.vertex(pose.pose(), (float) p.x, (float) p.y, (float) p.z)
                .color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, (int) (Mth.clamp(alpha, 0, 1) * 255))
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(pose.normal(), 0, 1, 0).endVertex();
    }
    private HolySwordVisualClient() {}
}
