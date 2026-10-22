package com.tonywww.slashblade_sendims.performance;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Preorder references only: changing a node's animation state remains immediately visible. */
public final class TopologySnapshot<T> {
    private static final int MAX_PARTS = 512;
    private final List<T> parts;
    private final List<Node<T>> nodes;

    private record Node<T>(T part, Map<?, T> children, Object[] order) {}

    private TopologySnapshot(List<T> parts, List<Node<T>> nodes) {
        this.parts = List.copyOf(parts);
        this.nodes = List.copyOf(nodes);
    }

    public static <T> TopologySnapshot<T> capture(T root, Function<T, Map<?, T>> children,
                                                 Predicate<T> supported) {
        List<T> parts = new ArrayList<>();
        List<Node<T>> nodes = new ArrayList<>();
        try {
            return collect(root, children, supported, parts, nodes, new IdentityHashMap<>())
                    ? new TopologySnapshot<>(parts, nodes) : null;
        } catch (ConcurrentModificationException ignored) {
            return null;
        }
    }

    private static <T> boolean collect(T part, Function<T, Map<?, T>> children, Predicate<T> supported,
                                       List<T> parts, List<Node<T>> nodes, IdentityHashMap<T, Boolean> path) {
        if (part == null || parts.size() >= MAX_PARTS || !supported.test(part) || path.put(part, true) != null) {
            return false;
        }
        Map<?, T> map = children.apply(part);
        Object[] order = map.values().toArray();
        parts.add(part);
        nodes.add(new Node<>(part, map, order));
        for (T child : map.values()) {
            if (!collect(child, children, supported, parts, nodes, path)) {
                return false;
            }
        }
        path.remove(part);
        return true;
    }

    /** Also detects same-size replacement and order changes; leaf validation allocates nothing. */
    public boolean matches(Function<T, Map<?, T>> children) {
        try {
            for (int i = 0; i < nodes.size(); i++) {
                Node<T> node = nodes.get(i);
                Map<?, T> map = children.apply(node.part());
                if (map != node.children() || map.size() != node.order().length) {
                    return false;
                }
                if (!map.isEmpty()) {
                    Iterator<T> iterator = map.values().iterator();
                    for (Object expected : node.order()) {
                        if (!iterator.hasNext() || iterator.next() != expected) {
                            return false;
                        }
                    }
                    if (iterator.hasNext()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (ConcurrentModificationException ignored) {
            return false;
        }
    }

    public Stream<T> stream() {
        return parts.stream();
    }
}
