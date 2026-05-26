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
 * Application service that validates UI requests before delegating to the provider port.
 */
public final class WeatherService implements AutoCloseable {
    public static final Duration MINIMUM_INTERVAL = Duration.ofSeconds(1);
    public static final Duration MAXIMUM_INTERVAL = Duration.ofHours(1);

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

    public List<ApiLimit> limits() {
        return provider.limits();
    }

    public MetricValue currentValue(String metricId) {
        requireMetric(metricId);
        return provider.currentValue(metricId);
    }

    public UUID subscribe(String metricId, Duration interval, WeatherUpdateListener listener) {
        requireMetric(metricId);
        validateInterval(interval);
        Objects.requireNonNull(listener, "listener");
        return provider.subscribe(metricId, interval, listener);
    }

    public void unsubscribe(UUID subscriptionId) {
        provider.unsubscribe(Objects.requireNonNull(subscriptionId, "subscriptionId"));
    }

    public List<SubscriptionSnapshot> subscriptions() {
        return provider.subscriptions();
    }

    public Optional<DataMetric> findMetric(String metricId) {
        return supportedMetrics().stream()
                .filter(metric -> metric.id().equals(metricId))
                .findFirst();
    }

    @Override
    public void close() {
        provider.close();
    }

    private void validateInterval(Duration interval) {
        Objects.requireNonNull(interval, "interval");
        if (interval.compareTo(MINIMUM_INTERVAL) < 0) {
            throw new IllegalArgumentException("interval must be at least " + MINIMUM_INTERVAL.toSeconds() + " second");
        }
        if (interval.compareTo(MAXIMUM_INTERVAL) > 0) {
            throw new IllegalArgumentException("interval must not exceed " + MAXIMUM_INTERVAL.toHours() + " hour");
        }
    }

    private void requireMetric(String metricId) {
        String normalized = Objects.requireNonNull(metricId, "metricId").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("metricId must not be blank");
        }
        if (findMetric(normalized).isEmpty()) {
            throw new IllegalArgumentException("unsupported metric: " + normalized);
        }
    }
}
