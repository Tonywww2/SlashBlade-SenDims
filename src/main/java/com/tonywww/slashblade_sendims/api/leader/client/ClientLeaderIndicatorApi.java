package com.tonywww.slashblade_sendims.api.leader.client;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-only extension point for rendering EXTERNAL Leader attack warnings.
 * Providers must be registered exactly once from a client setup path. They are queried
 * frequently and must remain fast, side-effect free, and safe for every living entity.
 */
public final class ClientLeaderIndicatorApi {
    private static final List<ExternalWarningProvider> EXTERNAL_WARNING_PROVIDERS =
            new CopyOnWriteArrayList<>();

    private ClientLeaderIndicatorApi() {
    }

    /**
     * Registers one provider for the lifetime of the client process. Providers are evaluated
     * in registration order; the first provider with a finite value controls the warning.
     * There is no unregister or duplicate-registration suppression.
     */
    public static void registerExternalWarningProvider(ExternalWarningProvider provider) {
        EXTERNAL_WARNING_PROVIDERS.add(Objects.requireNonNull(provider, "provider"));
    }

    /**
     * Returns the first visible warning progress, clamped to the inclusive range 0..1.
     * Non-finite values are ignored. A provider must never return {@code null}.
     */
    public static OptionalDouble getExternalWarningProgress(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        for (ExternalWarningProvider provider : EXTERNAL_WARNING_PROVIDERS) {
            OptionalDouble progress = Objects.requireNonNull(
                    provider.getProgress(entity), "provider result");
            if (progress.isPresent() && Double.isFinite(progress.getAsDouble())) {
                return OptionalDouble.of(Math.max(0.0, Math.min(1.0, progress.getAsDouble())));
            }
        }
        return OptionalDouble.empty();
    }

    @FunctionalInterface
    public interface ExternalWarningProvider {
        /**
         * Returns normalized warning progress, or empty when this provider has no visible warning.
         * Implementations must not mutate game state, render, play sounds, or send network traffic.
         */
        OptionalDouble getProgress(LivingEntity entity);
    }
}