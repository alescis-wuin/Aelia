package fr.alescis.aelia.ports;

import fr.alescis.aelia.model.MetricValue;

import java.util.UUID;

/**
 * Callback used by weather data providers to push periodic values.
 */
public interface WeatherUpdateListener {
    void onUpdate(UUID subscriptionId, MetricValue value);

    void onError(UUID subscriptionId, Throwable error);
}
