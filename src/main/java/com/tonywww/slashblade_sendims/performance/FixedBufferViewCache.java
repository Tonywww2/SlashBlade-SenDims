package com.tonywww.slashblade_sendims.performance;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Reuses immutable singleton views; never pools, closes or changes a buffer's lifetime. */
public final class FixedBufferViewCache<T> {
    private final IdentityHashMap<T, Set<T>> views = new IdentityHashMap<>();

    public Set<T> view(T value) {
        Set<T> cached = views.get(value);
        if (cached != null) return cached;
        Set<T> result = Collections.singleton(value);
        if (views.size() < 64) views.put(value, result);
        return result;
    }

    public void clear() { views.clear(); }
    public int size() { return views.size(); }
}
