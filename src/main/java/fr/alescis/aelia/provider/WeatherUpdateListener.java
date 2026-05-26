package fr.alescis.aelia.provider;

import fr.alescis.aelia.model.MetricReading;

/**
 * Listener notified by providers when a subscribed metric changes.
 */
@FunctionalInterface
public interface WeatherUpdateListener {
    void onUpdate(MetricReading reading);
}
