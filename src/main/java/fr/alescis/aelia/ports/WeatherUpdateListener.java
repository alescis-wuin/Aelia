package fr.alescis.aelia.ports;

import fr.alescis.aelia.model.MetricValue;

import java.util.UUID;

/**
 * Listener notified by a provider subscription.
 */
public interface WeatherUpdateListener {
    void onUpdate(UUID subscriptionId, MetricValue value);

    default void onError(UUID subscriptionId, Throwable error) {
        if (error instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException(error);
    }
}
