package com.tonywww.slashblade_sendims.client;

import net.minecraft.world.phys.Vec3;

/** Deterministic bounded mesh generation; no particles, lights, entities or per-frame random state. */
public final class HolySwordGeometry {
    public enum Kind { CHARGE, OPEN, CALIBURN, EXCALIBUR }
    public record Frame(Kind kind, Vec3 origin, Vec3 end, float height, float age,
            float pulseAge, int phase, long seed, boolean near, boolean firstPerson,
            boolean sky, float intensity, int moteLimit) {}
    @FunctionalInterface public interface Sink {
        void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha, int sprite);
    }
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final int GOLD = 0xFFF0C2, WHITE = 0xFFFFF4, WIND = 0xD5F5FF;
    private static final double TAU = Math.PI * 2;
    private static final int CALIBURN_WAVE_TICKS = 12;
    private static final int CALIBURN_SWORD_TICKS = 20;
    private HolySwordGeometry() {}

    public static int lifetime(Kind kind) {
        return switch (kind) { case CHARGE -> 24; case OPEN -> 8; case CALIBURN -> CALIBURN_SWORD_TICKS; case EXCALIBUR -> 30; };
    }
    public static int moteCount(Kind kind, boolean ex, boolean near, int remaining) {
        if (!near) return 0;
        return Math.max(0, Math.min(remaining, kind == Kind.OPEN ? 24 : ex ? 256 : 160));
    }
    public static double noise(long seed, int index) {
        long x = seed + index * 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return ((x ^ (x >>> 31)) >>> 11) * 0x1.0p-53;
    }
    private static double clamp(double n, double lo, double hi) { return Math.max(lo, Math.min(hi, n)); }
    private static Vec3 direction(Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        return d.lengthSqr() < 1e-8 ? new Vec3(0, 0, 1) : d.normalize();
    }
    private static Vec3 side(Vec3 axis) {
        Vec3 s = axis.cross(UP);
        return s.lengthSqr() < 1e-8 ? new Vec3(1, 0, 0) : s.normalize();
    }

    public static int draw(Frame f, boolean ex, Vec3 camera, Sink sink) {
        if (f.age < 0 || f.intensity <= 0) return 0;
        Vec3 axis = direction(f.origin, f.end), right = side(axis), up = right.cross(axis).normalize();
        double fade = f.kind == Kind.CHARGE ? 1 : f.kind == Kind.EXCALIBUR
                ? clamp((30 - f.age) / 20, 0, 1) : clamp(1 - f.age /
                (f.kind == Kind.CALIBURN ? CALIBURN_WAVE_TICKS : lifetime(f.kind)), 0, 1);
        // Near-camera shells stay transparent; the narrow white core remains readable.
        double shell = f.firstPerson ? 0.38 : 1;
        float opacity = (float) (fade * f.intensity);
        switch (f.kind) {
            case CHARGE -> {
                double progress = clamp(f.age / (ex ? 18 : 9), 0, 1);
                Vec3 tip = f.origin.add(axis.scale(0.25 + progress * 1.35));
                line(sink, f.origin.add(axis.scale(0.25)), tip, right, up,
                        0.025 + progress * 0.11, WHITE, (float) (0.8 * progress * f.intensity));
                line(sink, f.origin.add(axis.scale(0.25)), tip, right, up,
                        0.15 + progress * 0.3, GOLD, (float) (0.35 * progress * shell * f.intensity));
                if (f.near) {
                    for (int i = 0; i < (ex ? 3 : 2); i++) {
                        helix(sink, f.origin.subtract(UP.scale(0.5)), UP, new Vec3(1, 0, 0),
                                new Vec3(0, 0, 1), 2, ex ? 1.3 : 0.9,
                                f.age * 0.22 + i * TAU / 3, ex ? WIND : GOLD, 0.3F * f.intensity, 16);
                    }
                }
            }
            case OPEN -> crescent(sink, f.origin, axis, right, UP, 2.5 + f.age * 0.24,
                    0.5 * fade, opacity);
            case CALIBURN -> {
                double grow = clamp(f.age / 2, 0.05, 1);
                Vec3 waveEnd = f.origin.lerp(f.end, grow);
                line(sink, f.origin, waveEnd, right, UP.scale(0.28), 1.6 * fade, GOLD,
                        (float) (0.45 * opacity * shell));
                line(sink, f.origin, waveEnd, right, UP.scale(0.28), 0.65 * fade, WHITE, 0.85F * opacity);
                if (f.sky && f.height > 2) sword(sink, f, side(direction(f.origin, camera)));
                ring(sink, f.origin.add(0, -1.22, 0), Math.min(16, f.age * 2.2),
                        f.near ? 0.7 : 0.25, GOLD, opacity * 0.7F, f.near ? 48 : 24);
                if (f.near) crescent(sink, waveEnd, UP, right, axis, 2.8 * fade, 0.35 * fade, opacity);
            }
            case EXCALIBUR -> {
                // Pulses change the SAME continuous beam. The bright core stays inside the damage corridor.
                double pulse = Math.exp(-Math.max(0, f.pulseAge) * 0.5);
                double radius = (1.1 + f.phase * 0.15 + pulse * 0.3) * Math.sqrt(fade);
                line(sink, f.origin, f.end, right, up, radius * 4.5, GOLD, (float) (0.24 * opacity * shell));
                line(sink, f.origin, f.end, right, up, radius * 2.1, GOLD, 0.55F * opacity);
                line(sink, f.origin, f.end, right, up, radius * 0.8, WHITE, 0.96F * opacity);
                if (f.near) {
                    for (int i = 0; i < 3; i++) helix(sink, f.origin, axis, right, up,
                            f.origin.distanceTo(f.end), 2.1 + pulse * 0.4, -f.age * 0.3 + i * TAU / 3,
                            i == 0 ? WIND : GOLD, (float) (0.55 * opacity * (i == 0 ? clamp(1 - f.age / 6, 0, 1) : 1)), 20);
                }
                if (f.sky && f.height > 2) {
                    Vec3 top = f.origin.add(0, f.height, 0);
                    line(sink, f.origin, top, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                            1.1 * Math.sqrt(fade), WHITE, opacity * 0.68F);
                    line(sink, f.origin, top, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                            3.5 * fade, GOLD, (float) (0.23 * opacity * shell));
                    if (f.near) for (int i = 0; i < 3; i++)
                        helix(sink, f.origin, UP, new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                                f.height, 2.6 * fade, i * TAU / 3 - f.age * 0.18, GOLD, opacity * 0.6F, 24);
                }
                ring(sink, f.origin.add(0, -1.22, 0), Math.min(24, f.age * 1.4),
                        f.near ? 0.8 : 0.3, GOLD, opacity * 0.55F, f.near ? 32 : 24);
                if (f.phase == 2) {
                    float impact = (float) clamp(1 - f.pulseAge / 10, 0, 1);
                    double r = 1.5 + Math.min(10, f.pulseAge * 0.8);
                    ring(sink, f.end.add(0, -0.8, 0), r * 1.5, 0.6, GOLD, impact * f.intensity, 32);
                    if (f.near) hemisphere(sink, f.end, axis, right, up, r, impact * 0.5F * f.intensity);
                }
            }
        }
        // Only the giant sword receives the longer tail; expired wave motes consume no shared budget.
        if (opacity <= 0.005F) return 0;
        int count = moteCount(f.kind, ex, f.near, f.moteLimit);
        Vec3 towardCamera = direction(f.origin, camera);
        Vec3 billboardX = side(towardCamera), billboardY = billboardX.cross(towardCamera).normalize();
        double radius = ex ? 12 : 8;
        for (int i = 0; i < count; i++) {
            double n = noise(f.seed, i * 4), angle = noise(f.seed, i * 4 + 1) * TAU;
            double cycle = (f.age * (f.kind == Kind.CHARGE ? 0.075 : 0.045) + n) % 1;
            Vec3 p;
            if (f.kind == Kind.CHARGE) {
                double r = radius * (1 - cycle) * (0.35 + noise(f.seed, i * 4 + 2) * 0.65);
                p = f.origin.add(Math.cos(angle) * r, -0.6 + cycle * 1.3 + Math.sin(angle * 3) * (1 - cycle),
                        Math.sin(angle) * r);
            } else if (ex) {
                p = f.origin.lerp(f.end, cycle).add(right.scale(Math.cos(angle + f.age * 0.15) * (1 + n * 2.8)))
                        .add(up.scale(Math.sin(angle + f.age * 0.15) * (1 + n * 2.8)));
            } else {
                p = f.origin.add(Math.cos(angle) * (1 + cycle * 8), cycle * 2,
                        Math.sin(angle) * (1 + cycle * 8));
            }
            double size = 0.025 + noise(f.seed, i * 4 + 3) * 0.065;
            if (f.kind == Kind.CHARGE) size *= 0.5 + cycle;
            panel(sink, p, billboardX.scale(size), billboardY.scale(size * 2.5),
                    i % 7 == 0 && ex ? WIND : WHITE, opacity * (float) Math.sin(cycle * Math.PI), 1);
        }
        return count;
    }

    private static void sword(Sink s, Frame f, Vec3 right) {
        // Point downward, with an upright grip ABOVE the guard. Translate the complete
        // sword during the drop; do not squash its proportions into a fading light column.
        double height = Math.min(64, f.height);
        double length = height * 0.82;
        double scale = length / 52;
        double settle = Math.min(0.2, height * 0.02);
        double drop = clamp(1 - f.age / 6, 0, 1);
        double tipY = settle + (height - length - settle) * drop * drop;
        Vec3 tip = f.origin.add(0, tipY, 0);
        Vec3 guard = tip.add(0, length * 0.76, 0);
        Vec3 shoulder = guard.subtract(UP.scale(0.4 * scale));
        Vec3 depth = right.cross(UP).normalize();
        double tail = clamp((f.age - 10) / (CALIBURN_SWORD_TICKS - 10), 0, 1);
        float a = (float) ((1 - tail * tail * (3 - 2 * tail)) * f.intensity);
        if (a <= 0.005F) return;

        // Four closed blade facets and a wider white ridge keep the silhouette legible.
        Vec3[] section = {right.scale(2.1 * scale), depth.scale(0.4 * scale),
                right.scale(-2.1 * scale), depth.scale(-0.4 * scale)};
        for (int i = 0; i < section.length; i++) {
            Vec3 p = section[i], q = section[(i + 1) % section.length];
            s.quad(tip.add(p.scale(0.025)), tip.add(q.scale(0.025)),
                    shoulder.add(q), shoulder.add(p), i % 2 == 0 ? WHITE : GOLD, a * 0.85F, 0);
        }
        s.quad(shoulder.add(section[3]), shoulder.add(section[2]), shoulder.add(section[1]),
                shoulder.add(section[0]), GOLD, a * 0.85F, 0);
        s.quad(tip.add(section[0].scale(0.025)), tip.add(section[1].scale(0.025)),
                tip.add(section[2].scale(0.025)), tip.add(section[3].scale(0.025)), WHITE, a * 0.85F, 0);
        line(s, tip.add(UP.scale(0.2 * scale)), shoulder, right, depth, 0.65 * scale, WHITE, a * 0.98F);
        s.quad(tip.subtract(right.scale(0.09 * scale)), tip.add(right.scale(0.09 * scale)),
                shoulder.add(right.scale(3.1 * scale)), shoulder.subtract(right.scale(3.1 * scale)),
                GOLD, a * 0.5F, 0);

        // The guard, grip and pommel have depth and caps rather than a single flat panel.
        box(s, guard, right, depth, 5.2 * scale, 0.35 * scale, 0.65 * scale, GOLD, a * 0.95F);
        double gripBottom = length * 0.76, gripTop = length * 0.95;
        box(s, tip.add(0, (gripBottom + gripTop) * 0.5, 0), right, depth,
                0.65 * scale, (gripTop - gripBottom) * 0.5, 0.65 * scale, GOLD, a * 0.95F);
        line(s, tip.add(0, gripBottom, 0), tip.add(0, gripTop, 0), right, depth,
                0.22 * scale, WHITE, a * 0.9F);
        box(s, tip.add(0, length * 0.97, 0), right, depth,
                0.9 * scale, length * 0.028, 0.9 * scale, WHITE, a * 0.95F);
    }
    private static void box(Sink s, Vec3 center, Vec3 right, Vec3 depth,
                            double width, double height, double thickness, int color, float alpha) {
        Vec3 x = right.scale(width), y = UP.scale(height), z = depth.scale(thickness);
        Vec3 a = center.subtract(x).subtract(y).subtract(z), b = center.add(x).subtract(y).subtract(z);
        Vec3 c = center.add(x).add(y).subtract(z), d = center.subtract(x).add(y).subtract(z);
        Vec3 e = center.subtract(x).subtract(y).add(z), f = center.add(x).subtract(y).add(z);
        Vec3 g = center.add(x).add(y).add(z), h = center.subtract(x).add(y).add(z);
        s.quad(a, b, c, d, color, alpha, 0); s.quad(f, e, h, g, color, alpha, 0);
        s.quad(e, a, d, h, color, alpha, 0); s.quad(b, f, g, c, color, alpha, 0);
        s.quad(d, c, g, h, color, alpha, 0); s.quad(e, f, b, a, color, alpha, 0);
    }
    private static void line(Sink s, Vec3 a, Vec3 b, Vec3 right, Vec3 up, double width, int color, float alpha) {
        for (Vec3 axis : new Vec3[]{right, up}) {
            Vec3 offset = axis.scale(width);
            s.quad(a.subtract(offset), a.add(offset), b.add(offset.scale(0.85)), b.subtract(offset.scale(0.85)),
                    color, alpha, 2);
        }
    }
    private static void panel(Sink s, Vec3 center, Vec3 x, Vec3 y, int color, float alpha, int sprite) {
        s.quad(center.subtract(x).subtract(y), center.add(x).subtract(y),
                center.add(x).add(y), center.subtract(x).add(y), color, alpha, sprite);
    }
    private static void ring(Sink s, Vec3 center, double radius, double width, int color, float alpha, int segments) {
        if (radius < 0.05) return;
        for (int i = 0; i < segments; i++) {
            double a = i * TAU / segments, b = (i + 1) * TAU / segments;
            s.quad(center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                    center.add(Math.cos(a) * (radius + width), 0, Math.sin(a) * (radius + width)),
                    center.add(Math.cos(b) * (radius + width), 0, Math.sin(b) * (radius + width)),
                    center.add(Math.cos(b) * radius, 0, Math.sin(b) * radius), color, alpha, 0);
        }
    }
    private static void crescent(Sink s, Vec3 center, Vec3 forward, Vec3 right, Vec3 normal,
                                  double radius, double width, float alpha) {
        for (int i = 0; i < 32; i++) {
            double a = -1.9 + i * 3.8 / 32, b = -1.9 + (i + 1) * 3.8 / 32;
            Vec3 p = forward.scale(Math.cos(a)).add(right.scale(Math.sin(a)));
            Vec3 q = forward.scale(Math.cos(b)).add(right.scale(Math.sin(b)));
            s.quad(center.add(p.scale(radius)), center.add(p.scale(radius + width)),
                    center.add(q.scale(radius + width)), center.add(q.scale(radius)), WHITE,
                    alpha * (float) Math.sin((i + 0.5) * Math.PI / 32), 0);
        }
    }
    private static void helix(Sink s, Vec3 origin, Vec3 axis, Vec3 right, Vec3 up, double length,
                              double radius, double phase, int color, float alpha, int steps) {
        for (int i = 0; i < steps; i++) {
            double a = i / (double) steps, b = (i + 1) / (double) steps;
            Vec3 p = origin.add(axis.scale(a * length)).add(right.scale(Math.cos(a * TAU * 1.6 + phase) * radius))
                    .add(up.scale(Math.sin(a * TAU * 1.6 + phase) * radius));
            Vec3 q = origin.add(axis.scale(b * length)).add(right.scale(Math.cos(b * TAU * 1.6 + phase) * radius))
                    .add(up.scale(Math.sin(b * TAU * 1.6 + phase) * radius));
            Vec3 edge = right.scale(0.12);
            s.quad(p.subtract(edge), p.add(edge), q.add(edge), q.subtract(edge), color,
                    alpha * (float) Math.sin((a + b) * 0.5 * Math.PI), 0);
        }
    }
    private static void hemisphere(Sink s, Vec3 origin, Vec3 axis, Vec3 right, Vec3 up, double r, float alpha) {
        for (int j = 0; j < 2; j++) for (int i = 0; i < 16; i++) {
            double a = i * TAU / 16, b = (i + 1) * TAU / 16;
            double c = j * Math.PI / 4, d = (j + 1) * Math.PI / 4;
            s.quad(dome(origin, axis, right, up, r, a, c), dome(origin, axis, right, up, r, b, c),
                    dome(origin, axis, right, up, r, b, d), dome(origin, axis, right, up, r, a, d),
                    j % 2 == 0 ? GOLD : WHITE, alpha * 0.35F, 0);
        }
    }
    private static Vec3 dome(Vec3 p, Vec3 axis, Vec3 right, Vec3 up, double r, double a, double h) {
        return p.add(axis.scale(Math.sin(h) * r)).add(right.scale(Math.cos(a) * Math.cos(h) * r))
                .add(up.scale(Math.sin(a) * Math.cos(h) * r));
    }
}
