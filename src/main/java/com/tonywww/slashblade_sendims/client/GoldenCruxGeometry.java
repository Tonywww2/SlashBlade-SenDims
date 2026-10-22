package com.tonywww.slashblade_sendims.client;

import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

/** Shared immutable meshes: build once, animate only growth/fade at render time. */
final class GoldenCruxGeometry {
    record Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, int color, float alpha, boolean glow) {}
    record Spike(Vec3 anchor, List<Face> mesh, boolean distant) {}
    private static final Vec3 UP = new Vec3(0, 1, 0);
    static final List<Spike> CROWN = crown(16, 8);
    static final List<Face> RING = ring(4, 0.09, 48);
    static final List<Face> LEAP = ring(0.6, 0.04, 16);
    static final List<Face> SPEAR = spear();

    private GoldenCruxGeometry() {}

    private static List<Spike> crown(int outer, int inner) {
        List<Spike> spikes = new ArrayList<>();
        for (int i = 0; i < outer; i++) spikes.add(spike(i * Math.PI * 2 / outer, 2.9,
                3.0 + (i % 3) * 0.25, 0.17, i % 4 != 3));
        for (int i = 0; i < inner; i++) spikes.add(spike((i + 0.5) * Math.PI * 2 / inner, 1.35,
                1.8 + (i % 2) * 0.35, 0.14, false));
        return List.copyOf(spikes);
    }

    private static Spike spike(double angle, double radius, double height, double width, boolean distant) {
        List<Face> mesh = new ArrayList<>();
        Vec3 radial = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        Vec3 tangent = radial.cross(UP);
        // Animate each spike around its own planted root, slightly inside the
        // collision surface. Scaling the whole crown would lift/drop its roots.
        Vec3 root = new Vec3(0, -0.06, 0);
        Vec3[] previous = null;
        Vec3 previousCore = null;
        for (int segment = 0; segment <= 3; segment++) {
            double t = segment / 3.0;
            // The inward hook recalls the weapon's curled golden barbs.
            Vec3 core = root.add(radial.scale(0.65 * Math.sin(t * Math.PI) - 0.45 * t * t))
                    .add(0, height * t, 0);
            double w = width * (1 - t) + 0.006;
            Vec3[] section = { core.add(radial.scale(w)), core.add(tangent.scale(w)),
                    core.subtract(radial.scale(w)), core.subtract(tangent.scale(w)) };
            if (previous != null) {
                for (int side = 0; side < 4; side++) {
                    int next = (side + 1) % 4;
                    face(mesh, previous[side], previous[next], section[next], section[side],
                            side == 0 ? 0xFFE18A : side == 1 ? 0xDDA12C : 0xA76819, 0.84F, false);
                }
                // A thin luminous seam and a wider black/orange flame ribbon; no billboard planes.
                ribbon(mesh, previousCore, core, tangent, w * 0.42 + 0.018, 0xFFF0A2, 0.7F, true);
                Vec3 flameLow = previousCore.add(radial.scale(-width * 1.2));
                Vec3 flameHigh = core.add(radial.scale(-width * 1.2)).add(0, 0.18 * t, 0);
                ribbon(mesh, flameLow, flameHigh, tangent, w * 1.9, 0xFFB82B, 0.26F, true);
                ribbon(mesh, flameLow, flameHigh, tangent, w * 0.75, 0x20130B, 0.72F, false);
            }
            previous = section;
            previousCore = core;
        }
        // One small backward-pointing tooth on each main barb.
        Vec3 toothRoot = root.add(0, height * 0.58, 0).add(radial.scale(0.38));
        Vec3 toothTip = toothRoot.subtract(radial.scale(0.48)).add(0, -0.28, 0);
        ribbon(mesh, toothRoot, toothTip, tangent, width * 0.7, 0xFFD269, 0.86F, false);
        ribbon(mesh, toothRoot, toothTip, UP, width * 0.35, 0xFFE69B, 0.65F, true);
        return new Spike(radial.scale(radius), List.copyOf(mesh), distant);
    }

    private static List<Face> spear() {
        List<Face> mesh = new ArrayList<>();
        Vec3 top = new Vec3(0, 0.85, 0), tip = new Vec3(0, -1.5, 0);
        Vec3[] section = { top.add(0.14, 0, 0), top.add(0, 0, 0.14),
                top.add(-0.14, 0, 0), top.add(0, 0, -0.14) };
        for (int i = 0; i < 4; i++) face(mesh, section[i], section[(i + 1) % 4], tip, tip,
                i % 2 == 0 ? 0xFFE58A : 0xAE6B19, 0.72F, false);
        ribbon(mesh, top, tip, new Vec3(1, 0, 0), 0.035, 0xFFF2A9, 0.8F, true);
        ribbon(mesh, top, tip, new Vec3(0, 0, 1), 0.035, 0xFFF2A9, 0.8F, true);
        return List.copyOf(mesh);
    }

    private static List<Face> ring(double radius, double width, int segments) {
        List<Face> mesh = new ArrayList<>();
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
            face(mesh, new Vec3(Math.cos(a) * (radius - width), 0.04, Math.sin(a) * (radius - width)),
                    new Vec3(Math.cos(b) * (radius - width), 0.04, Math.sin(b) * (radius - width)),
                    new Vec3(Math.cos(b) * (radius + width), 0.04, Math.sin(b) * (radius + width)),
                    new Vec3(Math.cos(a) * (radius + width), 0.04, Math.sin(a) * (radius + width)),
                    0xFFC745, 0.52F, true);
        }
        return List.copyOf(mesh);
    }

    private static void ribbon(List<Face> mesh, Vec3 from, Vec3 to, Vec3 side, double width,
            int rgb, float alpha, boolean glow) {
        Vec3 offset = side.scale(width);
        face(mesh, from.add(offset), from.subtract(offset), to.subtract(offset.scale(0.5)),
                to.add(offset.scale(0.5)), rgb, alpha, glow);
    }

    private static void face(List<Face> mesh, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
            int rgb, float alpha, boolean glow) {
        Vec3 normal = b.subtract(a).cross(c.subtract(a)).normalize();
        mesh.add(new Face(a, b, c, d, normal, rgb, alpha, glow));
    }
}
