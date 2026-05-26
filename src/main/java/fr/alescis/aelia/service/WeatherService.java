package fr.alescis.aelia.service;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.MetricReading;
import fr.alescis.aelia.model.SubscriptionHandle;
import fr.alescis.aelia.model.SubscriptionRequest;
import fr.alescis.aelia.model.WeatherMetric;
import fr.alescis.aelia.provider.WeatherUpdateListener;

import java.util.List;

/**
 * Read model and subscription service used by controllers and views.
 */
public interface WeatherService extends AutoCloseable {
    DashboardSnapshot currentSnapshot();

    List<WeatherMetric> supportedMetrics();

    List<ApiLimit> apiLimits();

    MetricReading currentValue(String metricId);

    SubscriptionHandle subscribe(SubscriptionRequest request, WeatherUpdateListener listener);

    void unsubscribe(SubscriptionHandle handle);

    @Override
    void close();
}
