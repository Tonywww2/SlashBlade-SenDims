package com.tonywww.slashblade_sendims.client.performance;

/** No global model references: snapshots die together with their owning baked model. */
public final class RenderResourceEpoch {
    private static volatile long generation;

    private RenderResourceEpoch() {}

    public static long current() {
        return generation;
    }

    public static void invalidate() {
        generation++;
    }
}
