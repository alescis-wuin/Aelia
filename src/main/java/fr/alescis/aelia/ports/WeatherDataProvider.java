package fr.alescis.aelia.ports;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.model.ProviderDescriptor;
import fr.alescis.aelia.model.SubscriptionSnapshot;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Port implemented by local or remote weather data providers.
 */
public interface WeatherDataProvider extends AutoCloseable {
    ProviderDescriptor descriptor();

    List<DataMetric> supportedMetrics();

    List<ApiLimit> limits();

    MetricValue currentValue(String metricId);

    UUID subscribe(String metricId, Duration interval, WeatherUpdateListener listener);

    void unsubscribe(UUID subscriptionId);

    List<SubscriptionSnapshot> subscriptions();

    @Override
    void close();
}
