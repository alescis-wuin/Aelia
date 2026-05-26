package fr.alescis.aelia.provider;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.MetricReading;
import fr.alescis.aelia.model.SubscriptionHandle;
import fr.alescis.aelia.model.SubscriptionRequest;
import fr.alescis.aelia.model.WeatherMetric;

import java.util.List;

/**
 * Port implemented by local simulators and future remote weather APIs.
 */
public interface WeatherDashboardProvider extends AutoCloseable {
    DashboardSnapshot currentSnapshot();

    List<WeatherMetric> supportedMetrics();

    List<ApiLimit> apiLimits();

    MetricReading currentValue(String metricId);

    SubscriptionHandle subscribe(SubscriptionRequest request, WeatherUpdateListener listener);

    void unsubscribe(SubscriptionHandle handle);

    @Override
    void close();
}
