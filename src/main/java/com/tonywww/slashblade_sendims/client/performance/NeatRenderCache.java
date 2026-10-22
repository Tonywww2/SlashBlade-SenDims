package com.tonywww.slashblade_sendims.client.performance;

import com.tonywww.slashblade_sendims.performance.DecimalTextCache;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.text.DecimalFormat;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Render-thread caches have explicit bounds and are discarded on resource/language reload. */
public final class NeatRenderCache {
    private static final ThreadLocal<NeatRenderCache> LOCAL = ThreadLocal.withInitial(NeatRenderCache::new);
    private final DecimalTextCache decimals = new DecimalTextCache();
    private final Map<ItemLike, ItemStack> icons = new IdentityHashMap<>();
    private final String[] armor = new String[257];
    private final Map<String, Integer> widths = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > 1024;
        }
    };
    private Font font;
    private String colorKey;
    private int color;
    private long epoch = RenderResourceEpoch.current();

    private NeatRenderCache() {}

    public static NeatRenderCache current() {
        NeatRenderCache cache = LOCAL.get();
        if (cache.epoch != RenderResourceEpoch.current()) {
            cache = new NeatRenderCache();
            LOCAL.set(cache);
        }
        return cache;
    }

    public DecimalFormat formatter(String pattern) {
        return decimals.formatter(pattern);
    }

    public String format(DecimalFormat source, double value) {
        return decimals.format(source, value);
    }

    public int width(Font currentFont, String text) {
        if (currentFont != font) {
            widths.clear();
            font = currentFont;
        }
        // Oversized/custom strings are measured normally instead of retained in this shared cache.
        if (text.length() > 256) {
            return currentFont.width(text);
        }
        Integer cached = widths.get(text);
        if (cached == null) {
            cached = currentFont.width(text);
            widths.put(text, cached);
        }
        return cached;
    }

    public int color(CharSequence text) {
        if (colorKey == null || !colorKey.contentEquals(text)) {
            color = HexFormat.fromHexDigits(text);
            colorKey = text.toString();
        }
        return color;
    }

    public String armorText(int value) {
        if (value < 0 || value >= armor.length) {
            return "×" + value;
        }
        if (armor[value] == null) {
            armor[value] = "×" + value;
        }
        return armor[value];
    }

    /** Private stacks are passed only to the read-only item-rendering path, never to inventories. */
    public ItemStack icon(ItemLike item) {
        ItemStack cached = icons.get(item);
        if (cached == null) {
            cached = new ItemStack(item);
            if (icons.size() < 16) {
                icons.put(item, cached);
            }
        }
        return cached;
    }
}
