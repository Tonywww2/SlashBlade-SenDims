package com.tonywww.slashblade_sendims.client;

import com.tonywww.slashblade_sendims.client.HolySwordGeometry.Frame;
import com.tonywww.slashblade_sendims.client.HolySwordGeometry.Kind;
import com.tonywww.slashblade_sendims.network.HolySwordVisualPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class HolySwordVisualSelfTest {
    private static long vertices;
    private static double checksum;
    private static void check(boolean ok, String reason) { if (!ok) throw new AssertionError(reason); }
    private static final HolySwordGeometry.Sink SINK = (a, b, c, d, color, alpha, mote) -> {
        check(Float.isFinite(alpha) && alpha >= 0, "finite opacity");
        for (Vec3 p : new Vec3[]{a, b, c, d}) {
            check(Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z), "finite vertices");
            check(Math.abs(p.x) < 200 && Math.abs(p.y) < 200 && Math.abs(p.z) < 200, "bounded geometry");
            checksum += p.x * .3 + p.y * .7 + p.z; vertices++;
        }
    };
    private static Frame frame(Kind kind, float height, float age, boolean near, int budget) {
        return new Frame(kind, Vec3.ZERO, new Vec3(0, 0, 36), height, age, 1, 2, 12345,
                near, false, true, 1, budget);
    }
    private static final class VisibleBounds implements HolySwordGeometry.Sink {
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double gripMinX = Double.POSITIVE_INFINITY, gripMaxX = Double.NEGATIVE_INFINITY;
        double gripMinZ = Double.POSITIVE_INFINITY, gripMaxZ = Double.NEGATIVE_INFINITY;
        double gripMinY = Double.POSITIVE_INFINITY, gripMaxY = Double.NEGATIVE_INFINITY;
        float peakAlpha;
        int quads;
        @Override public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int color, float alpha, int sprite) {
            if (alpha <= .005F) return;
            quads++; peakAlpha = Math.max(peakAlpha, alpha);
            for (Vec3 v : new Vec3[]{a,b,c,d}) {
                minY = Math.min(minY, v.y); maxY = Math.max(maxY, v.y);
                // Above the guard: check that a long, volumetric handle is actually present.
                if ((a.y + b.y + c.y + d.y) / 4 > 42) {
                    gripMinX = Math.min(gripMinX, v.x); gripMaxX = Math.max(gripMaxX, v.x);
                    gripMinZ = Math.min(gripMinZ, v.z); gripMaxZ = Math.max(gripMaxZ, v.z);
                    gripMinY = Math.min(gripMinY, v.y); gripMaxY = Math.max(gripMaxY, v.y);
                }
            }
        }
    }
    private static VisibleBounds swordAt(float height, float age, Vec3 camera) {
        VisibleBounds bounds = new VisibleBounds();
        int motes = HolySwordGeometry.draw(frame(Kind.CALIBURN, height, age, true, 768), false, camera, bounds);
        if (age >= 12) check(motes == 0, "longer sword tail must not extend the mote budget");
        return bounds;
    }
    private static void giantSword() {
        check(HolySwordGeometry.lifetime(Kind.CALIBURN) == 20, "one-second giant sword lifetime");
        for (Vec3 camera : new Vec3[]{new Vec3(0,20,-60), new Vec3(60,20,0), new Vec3(40,20,40)}) {
            VisibleBounds late = swordAt(64,14,camera);
            check(late.quads > 0 && late.minY < .3 && late.maxY > 52, "complete sword remains after old expiry");
            check(late.gripMaxY - late.gripMinY > 6, "upright handle above the guard");
            check(late.gripMaxX - late.gripMinX > 1 && late.gripMaxZ - late.gripMinZ > 1,
                    "handle has thickness from different viewing directions");
            VisibleBounds landed = swordAt(64,12,camera);
            check(Math.abs(landed.minY-late.minY) < 1e-6 && Math.abs(landed.maxY-late.maxY) < 1e-6,
                    "settled sword keeps full proportions while fading");
            VisibleBounds bright = swordAt(64,8,camera), dim = swordAt(64,18,camera);
            check(bright.peakAlpha > .95F && late.peakAlpha > .6F && dim.peakAlpha < .15F,
                    "bright hold then smooth fade");
            check(swordAt(64,20,camera).quads == 0, "sword vanishes at the end of the tail");
        }
        for (float height : new float[]{3,10,64}) for (float age : new float[]{0,3,14}) {
            // Isolate the sky mesh from the separate ground impact crescent and motes.
            VisibleBounds bounds = new VisibleBounds();
            HolySwordGeometry.draw(frame(Kind.CALIBURN,height,age,false,0),false,new Vec3(4,2,-4),bounds);
            check(bounds.maxY <= height + .01, "complete sword fits under its ceiling throughout the drop");
        }
    }
    public static void main(String[] args) {
        var p = new HolySwordVisualPacket(new ResourceLocation("minecraft:the_end"), UUID.randomUUID(), 17,
                123456L, HolySwordVisualPacket.Stage.PULSE_2, true, Vec3.ZERO, new Vec3(0, 0, 36), 96, 18);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            HolySwordVisualPacket.encode(p, buf);
            check(p.equals(HolySwordVisualPacket.decode(buf)), "packet round trip");
            check(buf.readableBytes() == 0 && p.valid(), "complete bounded descriptor");
        } finally { buf.release(); }
        check(!new HolySwordVisualPacket(p.dimension(), p.user(), 0, 0, p.stage(), true,
                new Vec3(Double.NaN, 0, 0), p.end(), 96, 0).valid(), "reject invalid origin");
        check(!new HolySwordVisualPacket(p.dimension(), p.user(), 0, 0, p.stage(), true,
                Vec3.ZERO, new Vec3(0, 0, 999), 96, 0).valid(), "reject unbounded effect");
        for (Kind k : Kind.values()) for (boolean near : new boolean[]{true, false}) {
            for (int age = 0; age < HolySwordGeometry.lifetime(k); age++) {
                long before = vertices;
                int motes = HolySwordGeometry.draw(frame(k, 96, age, near, 768), true, new Vec3(4, 2, -4), SINK);
                check(motes <= 256 && (near || motes == 0), "distance LOD mote limit");
                check(vertices - before < 2400, "less than 600 quads per cast");
            }
        }
        int remaining = 768;
        for (int i = 0; i < 8; i++) remaining -= HolySwordGeometry.draw(
                frame(Kind.EXCALIBUR, 96, 11, true, remaining), true, new Vec3(4, 2, -4), SINK);
        check(remaining == 0, "global 768 mote budget across simultaneous casts");
        check(HolySwordGeometry.moteCount(Kind.CALIBURN, false, true, 768) == 160, "Caliburn 160 mote cap");
        long[] skyVertices = {0};
        HolySwordGeometry.Sink ceiling = (a, b, c, d, color, alpha, mote) -> {
            for (Vec3 v : new Vec3[]{a,b,c,d}) check(v.y <= 2.01, "low ceiling contains sky geometry");
            skyVertices[0] += 4;
        };
        HolySwordGeometry.draw(frame(Kind.CALIBURN, 2, 1, false, 0), false, new Vec3(4,2,-4), ceiling);
        giantSword();
        // Degenerate and vertical beams must not produce NaNs or disappear from invalid orientation bases.
        for (Vec3 end : new Vec3[]{Vec3.ZERO, new Vec3(0,36,0), new Vec3(0,-36,0)}) {
            HolySwordGeometry.draw(new Frame(Kind.EXCALIBUR, Vec3.ZERO,end,0,5,1,1,0,true,true,false,1,32),
                    true, new Vec3(0,5,0), SINK);
        }
        double n = HolySwordGeometry.noise(10, 5);
        check(n == HolySwordGeometry.noise(10,5) && n >= 0 && n < 1, "deterministic client seeds");
        System.out.println("Holy sword VFX: packet, geometry, upright giant sword, fade, ceiling, vertical aiming and shared budget checks passed.");
        // CPU mesh generation only: not a GPU/FPS claim. Warm up before measuring four simultaneous casts.
        var sample = frame(Kind.EXCALIBUR,96,11,true,192);
        HolySwordGeometry.Sink bench = (a,b,c,d,color,alpha,mote) -> checksum += a.x + b.y + c.z + d.x;
        for (int i=0;i<2000;i++) HolySwordGeometry.draw(sample,true,new Vec3(4,2,-4),bench);
        long start=System.nanoTime();
        for(int i=0;i<2000;i++) for(int j=0;j<4;j++) HolySwordGeometry.draw(sample,true,new Vec3(4,2,-4),bench);
        System.out.printf("Four-cast mesh generation average: %.3f ms (CPU only); checksum %.2f%n",
                (System.nanoTime()-start)/2_000_000_000.0, checksum);
    }
}
