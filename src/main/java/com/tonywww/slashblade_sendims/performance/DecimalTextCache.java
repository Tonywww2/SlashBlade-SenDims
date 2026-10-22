package com.tonywww.slashblade_sendims.performance;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;

/** Bounded, thread-confined cache. Formatting rules and the FORMAT locale are live inputs. */
public final class DecimalTextCache {
    private static final int SIZE = 128;
    private final long[] keys = new long[SIZE];
    private final String[] values = new String[SIZE];
    private String pattern;
    private Locale locale;
    private DecimalFormat formatter;

    public DecimalFormat formatter(String requestedPattern) {
        Locale requestedLocale = Locale.getDefault(Locale.Category.FORMAT);
        if (formatter == null || !requestedPattern.equals(pattern) || !requestedLocale.equals(locale)) {
            formatter = new DecimalFormat(requestedPattern);
            pattern = requestedPattern;
            locale = requestedLocale;
            Arrays.fill(values, null);
        }
        return formatter;
    }

    public String format(DecimalFormat source, double value) {
        if (source != formatter) {
            return source.format(value);
        }
        long key = Double.doubleToLongBits(value);
        int slot = (int) (key ^ (key >>> 32) ^ (key >>> 48)) & (SIZE - 1);
        if (values[slot] == null || keys[slot] != key) {
            keys[slot] = key;
            values[slot] = source.format(value);
        }
        return values[slot];
    }
}
