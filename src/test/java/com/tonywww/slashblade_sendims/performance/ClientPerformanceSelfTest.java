package com.tonywww.slashblade_sendims.performance;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Boundary/behavior checks; does not need a window, world, or Minecraft static bootstrap. */
public final class ClientPerformanceSelfTest {
    private static int checks;

    private static void check(boolean condition, String reason) {
        checks++;
        if (!condition) {
            throw new AssertionError(reason);
        }
    }

    private static final class Part {
        private Map<String, Part> children = new LinkedHashMap<>();
        private float rotation;
        private boolean supported = true;
    }

    private static TopologySnapshot<Part> capture(Part root) {
        return TopologySnapshot.capture(root, part -> part.children, part -> part.supported);
    }

    private static void topology() {
        Part root = new Part();
        Part arm = new Part();
        Part hand = new Part();
        Part leg = new Part();
        root.children.put("arm", arm);
        arm.children.put("hand", hand);
        root.children.put("leg", leg);
        TopologySnapshot<Part> snapshot = capture(root);
        check(snapshot != null && snapshot.stream().toList().equals(List.of(root, arm, hand, leg)), "preorder");
        check(snapshot.matches(part -> part.children), "unchanged topology");
        arm.rotation = 1.5f;
        check(snapshot.stream().skip(1).findFirst().orElseThrow().rotation == 1.5f, "live animation pose");
        check(snapshot.matches(part -> part.children), "animation does not invalidate topology");
        arm.children.put("hand", new Part());
        check(!snapshot.matches(part -> part.children), "nested same-size replacement");
        snapshot = capture(root);
        root.children.remove("arm");
        root.children.put("arm", arm);
        check(!snapshot.matches(part -> part.children), "same-size reordering");
        snapshot = capture(root);
        arm.children.clear();
        check(!snapshot.matches(part -> part.children), "nested removal");
        snapshot = capture(root);
        leg.children.put("foot", new Part());
        check(!snapshot.matches(part -> part.children), "nested addition");
        snapshot = capture(root);
        root.children = new LinkedHashMap<>(root.children);
        check(!snapshot.matches(part -> part.children), "map identity replacement");
        arm.supported = false;
        check(capture(root) == null, "custom traversal conservative fallback");
        arm.supported = true;
        arm.children.put("cycle", root);
        check(capture(root) == null, "cyclic topology bounded fallback");
        root = new Part();
        for (int i = 0; i < 512; i++) {
            root.children.put("part" + i, new Part());
        }
        check(capture(root) == null, "oversized topology bounded fallback");
        check(capture(new Part()).stream().count() == 1, "leaf traversal");
    }

    private static void decimals() {
        Locale previous = Locale.getDefault(Locale.Category.FORMAT);
        try {
            DecimalTextCache cache = new DecimalTextCache();
            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            DecimalFormat formatter = cache.formatter("#.##");
            check(formatter == cache.formatter("#.##"), "reuse formatter");
            double[] values = {0, -0.0, 0.125, 20, 99999.987, Double.NaN, Double.POSITIVE_INFINITY};
            for (double value : values) {
                check(cache.format(formatter, value).equals(new DecimalFormat("#.##").format(value)), "format parity");
            }
            for (int i = 0; i < 2000; i++) {
                double value = i / 7.0;
                check(cache.format(formatter, value).equals(formatter.format(value)), "collision/eviction parity");
            }
            DecimalFormat changed = cache.formatter("0.000");
            check(changed != formatter && cache.format(changed, 20).equals("20.000"), "pattern change invalidation");
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            DecimalFormat localized = cache.formatter("0.000");
            check(localized != changed && cache.format(localized, 20.5).equals("20,500"), "locale change invalidation");
            DecimalFormat external = new DecimalFormat("0.0");
            check(cache.format(external, 4.25).equals(external.format(4.25)), "foreign formatter fallback");
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous);
        }
    }

    private static void loadedMods() {
        Map<String, String> input = new HashMap<>();
        input.put("oculus", "1.8.0");
        input.put("immediatelyfast", "1.5.2");
        LoadedModSnapshot snapshot = new LoadedModSnapshot(input);
        input.clear();
        check(snapshot.isLoaded("oculus") && snapshot.isLoaded("immediatelyfast"), "present optional mods");
        check(!snapshot.isLoaded("missing") && !snapshot.isLoaded(null), "absent optional mods");
        check(snapshot.version("oculus").equals("1.8.0"), "version snapshot is immutable");
        check(snapshot.version("missing").equals("unknown") && snapshot.version(null).equals("unknown"), "unknown versions");
        LoadedModSnapshot absent = new LoadedModSnapshot(Map.of());
        check(!absent.isLoaded("oculus") && !absent.isLoaded("immediatelyfast"), "both optional mods absent");
    }

    private static void fluidMap() {
        // Compare Forge's original eagerly sized table against the replacement through growth/clear/reuse.
        Object2ObjectArrayMap<Object, Double> original = new Object2ObjectArrayMap<>(1024);
        Object2ObjectArrayMap<Object, Double> lazy = new Object2ObjectArrayMap<>();
        check(original.equals(lazy), "dry scan table parity");
        List<Object> fluids = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            Object fluid = new Object();
            fluids.add(fluid);
            original.put(fluid, i / 13.0);
            lazy.put(fluid, i / 13.0);
            check(original.equals(lazy), "multi-fluid growth preserves contents");
            check(new ArrayList<>(original.keySet()).equals(new ArrayList<>(lazy.keySet())), "fluid iteration order");
        }
        original.put(fluids.get(4), 1.25);
        lazy.put(fluids.get(4), 1.25);
        check(original.equals(lazy), "fluid height update parity");
        original.clear();
        lazy.clear();
        check(original.equals(lazy) && lazy.isEmpty(), "empty table after clear");
    }

    public static void main(String[] args) {
        topology();
        decimals();
        loadedMods();
        fluidMap();
        System.out.println("Client performance checks passed: " + checks);
    }
}
