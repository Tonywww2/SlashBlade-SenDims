package com.tonywww.slashblade_sendims.performance;

import java.util.Arrays;

/** A bounded, allocation-free lookup for immutable text/width pairs. Render-thread owned. */
public final class TextResultCache {
    private static final int CAPACITY = 128;
    private final String[] inputs = new String[CAPACITY];
    private final String[] results = new String[CAPACITY];
    private final int[] widths = new int[CAPACITY];

    private int slot(String text, int width) {
        int hash = text.hashCode() * 31 + width;
        return (hash ^ (hash >>> 16)) & (CAPACITY - 1);
    }

    public String get(String text, int width) {
        if (text == null || text.length() > 512) return null;
        int index = slot(text, width);
        return widths[index] == width && text.equals(inputs[index]) ? results[index] : null;
    }

    public void put(String text, int width, String result) {
        if (text == null || text.length() > 512 || result == null || result.length() > 515) return;
        int index = slot(text, width);
        inputs[index] = text;
        widths[index] = width;
        results[index] = result;
    }

    public void clear() {
        Arrays.fill(inputs, null);
        Arrays.fill(results, null);
    }
}
