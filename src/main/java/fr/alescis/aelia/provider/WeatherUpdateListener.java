package fr.alescis.aelia.provider;

import fr.alescis.aelia.model.MetricReading;

/**
 * Observer notified when a provider publishes a metric reading.
 */
@FunctionalInterface
public interface WeatherUpdateListener {
    void onUpdate(MetricReading reading);
}
