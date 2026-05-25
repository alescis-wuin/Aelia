package fr.alescis.aelia.service;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.model.ProviderDescriptor;
import fr.alescis.aelia.model.SubscriptionSnapshot;
import fr.alescis.aelia.ports.WeatherDataProvider;
import fr.alescis.aelia.ports.WeatherUpdateListener;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service that centralizes validation and provider access.
 */
public final class WeatherService implements AutoCloseable {

    private static final Duration MINIMUM_INTERVAL = Duration.ofSeconds(1);
    private static final Duration MAXIMUM_INTERVAL = Duration.ofHours(24);

    private final WeatherDataProvider provider;

    public WeatherService(WeatherDataProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    public ProviderDescriptor descriptor() {
        return provider.descriptor();
    }

    public List<DataMetric> supportedMetrics() {
        return provider.supportedMetrics();
    }

    public Optional<DataMetric> findMetric(String metricId) {
        Objects.requireNonNull(metricId, "metricId");
        return supportedMetrics().stream()
                .filter(metric -> metric.id().equals(metricId))
                .findFirst();
    }

    public List<ApiLimit> limits() {
        return provider.limits();
    }

    public MetricValue currentValue(String metricId) {
        requireMetric(metricId);
        return provider.currentValue(metricId);
    }

    public UUID subscribe(String metricId, Duration interval, WeatherUpdateListener listener) {
        requireMetric(metricId);
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(listener, "listener");
        if (interval.compareTo(MINIMUM_INTERVAL) < 0 || interval.compareTo(MAXIMUM_INTERVAL) > 0) {
            throw new IllegalArgumentException("Interval must be between 1 second and 24 hours.");
        }
        return provider.subscribe(metricId, interval, listener);
    }

    public void unsubscribe(UUID subscriptionId) {
        provider.unsubscribe(Objects.requireNonNull(subscriptionId, "subscriptionId"));
    }

    public List<SubscriptionSnapshot> activeSubscriptions() {
        return provider.activeSubscriptions();
    }

    @Override
    public void close() {
        provider.close();
    }

    private void requireMetric(String metricId) {
        if (findMetric(metricId).isEmpty()) {
            throw new IllegalArgumentException("Unsupported metric: " + metricId);
        }
    }
}
