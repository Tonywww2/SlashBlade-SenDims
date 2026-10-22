package com.tonywww.slashblade_sendims.startup;

import java.util.IdentityHashMap;
import java.util.Objects;

/** Bounded memo for immutable results, retained only while a resource reload is active. */
public final class ScopedIdentityCache<K, C, V> {
    private record Entry<C, V>(C context, V value) {}
    private IdentityHashMap<K, Entry<C, V>> values = new IdentityHashMap<>();
    private long generation;
    private boolean active;
    private int limit;

    public synchronized long begin(int capacity) {
        values = new IdentityHashMap<>(); limit = Math.max(0, capacity); active = true;
        return ++generation;
    }

    public synchronized V get(K key, C context) {
        if (!active) return null;
        Entry<C, V> entry = values.get(key);
        return entry != null && Objects.equals(context, entry.context()) ? entry.value() : null;
    }

    public synchronized void put(K key, C context, V value) {
        if (active && value != null && (values.size() < limit || values.containsKey(key))) {
            values.put(key, new Entry<>(context, value));
        }
    }

    public synchronized void finish(long token) {
        if (token != generation) return;
        values = new IdentityHashMap<>(); active = false;
    }

    public synchronized int size() { return values.size(); }
}
