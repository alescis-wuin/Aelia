package fr.alescis.aelia.service;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.MetricReading;
import fr.alescis.aelia.model.SubscriptionHandle;
import fr.alescis.aelia.model.SubscriptionRequest;
import fr.alescis.aelia.model.WeatherMetric;
import fr.alescis.aelia.provider.WeatherDashboardProvider;
import fr.alescis.aelia.provider.WeatherUpdateListener;

import java.util.List;
import java.util.Objects;

/**
 * Application service that isolates controllers and views from provider implementations.
 */
public final class AeliaWeatherService implements WeatherService {
    private final WeatherDashboardProvider provider;

    public AeliaWeatherService(WeatherDashboardProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public DashboardSnapshot currentSnapshot() {
        return provider.currentSnapshot();
    }

    @Override
    public List<WeatherMetric> supportedMetrics() {
        return provider.supportedMetrics();
    }

    @Override
    public List<ApiLimit> apiLimits() {
        return provider.apiLimits();
    }

    @Override
    public MetricReading currentValue(String metricId) {
        return provider.currentValue(metricId);
    }

    @Override
    public SubscriptionHandle subscribe(SubscriptionRequest request, WeatherUpdateListener listener) {
        return provider.subscribe(request, listener);
    }

    @Override
    public void unsubscribe(SubscriptionHandle handle) {
        provider.unsubscribe(handle);
    }

    @Override
    public void close() {
        provider.close();
    }
}
