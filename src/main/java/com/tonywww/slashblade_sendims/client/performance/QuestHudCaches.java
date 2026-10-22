package com.tonywww.slashblade_sendims.client.performance;

import com.tonywww.slashblade_sendims.performance.TextResultCache;
import net.minecraft.client.gui.Font;

public final class QuestHudCaches {
    private static final TextResultCache TEXT = new TextResultCache();
    private static Font font;
    private static long epoch = -1;

    private QuestHudCaches() {}

    private static void prepare(Font current) {
        long generation = RenderResourceEpoch.current();
        if (font != current || epoch != generation) {
            TEXT.clear(); font = current; epoch = generation;
        }
    }

    public static String get(Font current, String text, int width) {
        prepare(current);
        String result = TEXT.get(text, width);
        if (result != null && ClientPerformanceOptions.diagnostics) RenderOptimizationCounters.trimmedTextHits++;
        return result;
    }

    public static void put(Font current, String text, int width, String result) {
        prepare(current);
        TEXT.put(text, width, result);
    }

    public static void clear() { TEXT.clear(); font = null; epoch = -1; }
}
