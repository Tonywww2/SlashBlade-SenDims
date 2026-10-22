package com.tonywww.slashblade_sendims.startup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/** O(workers) queued jobs; no worker blocks waiting for another job in the same executor. */
public final class BoundedPreparation {
    private BoundedPreparation() {}

    public static <T> CompletableFuture<List<T>> map(int count, int workers, Executor executor, IntFunction<T> operation) {
        if (count < 0 || workers < 1) throw new IllegalArgumentException("Invalid preparation size/budget");
        if (count == 0) return CompletableFuture.completedFuture(List.of());
        Object[] results = new Object[count];
        AtomicInteger next = new AtomicInteger();
        List<CompletableFuture<Void>> jobs = new ArrayList<>(Math.min(workers, count));
        for (int worker = 0; worker < Math.min(workers, count); worker++) {
            jobs.add(CompletableFuture.runAsync(() -> {
                for (int index; (index = next.getAndIncrement()) < count;) results[index] = operation.apply(index);
            }, executor));
        }
        return CompletableFuture.allOf(jobs.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<T> output = new ArrayList<>(count);
            for (Object result : results) {
                @SuppressWarnings("unchecked") T value = (T) result;
                output.add(value);
            }
            return output;
        });
    }
}
