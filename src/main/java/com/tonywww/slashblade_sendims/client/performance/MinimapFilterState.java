package com.tonywww.slashblade_sendims.client.performance;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tonywww.slashblade_sendims.performance.TextureFilterMemo;

/** Scope is one FTB HUD invocation, not an assumption about global GL state across frames. */
public final class MinimapFilterState {
    private static final TextureFilterMemo MEMO = new TextureFilterMemo();
    private static int depth;
    private static int bound = -1;
    private static boolean ownWrite;

    private MinimapFilterState() {}

    public static void begin() { depth++; bound = -1; MEMO.clear(); }
    public static void end() { depth = Math.max(0, depth - 1); bound = -1; MEMO.clear(); }
    public static void bind(int texture) { bound = texture; }
    public static void beginIcon() { bound = -1; }
    public static void foreignWrite() { if (depth > 0 && !ownWrite) MEMO.clear(); }
    public static void clear() { depth = 0; bound = -1; MEMO.clear(); }

    public static void parameter(int target, int parameter, int value) {
        if (depth == 0 || !ClientPerformanceOptions.minimapFilters || target != 0x0DE1 || bound < 0) {
            RenderSystem.texParameter(target, parameter, value);
            return;
        }
        if (!MEMO.needsWrite(bound, parameter, value)) {
            if (ClientPerformanceOptions.diagnostics) RenderOptimizationCounters.skippedTextureWrites++;
            return;
        }
        ownWrite = true;
        try {
            RenderSystem.texParameter(target, parameter, value);
            MEMO.record(bound, parameter, value);
        } finally {
            ownWrite = false;
        }
    }
}
