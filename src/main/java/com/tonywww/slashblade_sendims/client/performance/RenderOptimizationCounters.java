package com.tonywww.slashblade_sendims.client.performance;

/** Optional smoke-test diagnostics. Counters are render-thread owned and disabled in normal play. */
public final class RenderOptimizationCounters {
    public static long grinderIdle;
    public static long grinderAnimated;
    public static long objFaces;
    public static long backgroundSlices;
    public static long trimmedTextHits;
    public static long skippedTextureWrites;
    public static long fixedBufferViews;

    private RenderOptimizationCounters() {}

    public static long[] snapshot() {
        return new long[]{grinderIdle, grinderAnimated, objFaces, backgroundSlices,
                trimmedTextHits, skippedTextureWrites, fixedBufferViews};
    }
}
