package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.model.ProviderDescriptor;
import fr.alescis.aelia.model.SubscriptionSnapshot;
import fr.alescis.aelia.ports.WeatherDataProvider;
import fr.alescis.aelia.ports.WeatherUpdateListener;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generic Open-Meteo data-provider adapter that reuses the dashboard provider mapping pipeline.
 */
public final class OpenMeteoWeatherDataProvider implements WeatherDataProvider {
    private static final ProviderDescriptor DESCRIPTOR = new ProviderDescriptor(
            "open-meteo-weather-provider",
            "Open-Meteo remote weather provider",
            "Weather forecast, air quality, pollen and geocoding data from Open-Meteo.",
            true,
            "0.3.17",
            Optional.of(URI.create("https://open-meteo.com/en/docs"))
    );
    private static final Duration MINIMUM_SUBSCRIPTION_INTERVAL = Duration.ofSeconds(30);

    private final OpenMeteoWeatherDashboardProvider dashboardProvider;
    private final OpenMeteoMapper mapper = new OpenMeteoMapper();
    private final ScheduledExecutorService executorService;
    private final Map<UUID, ActiveSubscription> subscriptions = new ConcurrentHashMap<>();

    public OpenMeteoWeatherDataProvider() {
        this(new OpenMeteoWeatherDashboardProvider());
    }

    public OpenMeteoWeatherDataProvider(OpenMeteoWeatherDashboardProvider dashboardProvider) {
        this.dashboardProvider = Objects.requireNonNull(dashboardProvider, "dashboardProvider");
        this.executorService = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "aelia-openmeteo-data-" + ThreadIds.NEXT.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public ProviderDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public List<DataMetric> supportedMetrics() {
        return OpenMeteoMetricCatalog.dataMetrics();
    }

    @Override
    public List<ApiLimit> limits() {
        return OpenMeteoMetricCatalog.limits();
    }

    @Override
    public MetricValue currentValue(String metricId) {
        String normalizedMetricId = normalizeMetricId(metricId);
        DashboardSnapshot snapshot = dashboardProvider.cachedOrCurrentSnapshot();
        DataMetric metric = metric(normalizedMetricId);
        return MetricValue.numeric(metric, Instant.now(), mapper.metricValue(normalizedMetricId, snapshot));
    }

    @Override
    public UUID subscribe(String metricId, Duration interval, WeatherUpdateListener listener) {
        Objects.requireNonNull(listener, "listener");
        String normalizedMetricId = normalizeMetricId(metricId);
        UUID id = UUID.randomUUID();
        ActiveSubscription subscription = new ActiveSubscription(id, normalizedMetricId, normalizedInterval(interval), Instant.now());
        Runnable task = () -> publish(subscription, listener);
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                task,
                0L,
                subscription.interval().toMillis(),
                TimeUnit.MILLISECONDS
        );
        subscription.future().set(future);
        subscriptions.put(id, subscription);
        return id;
    }

    @Override
    public void unsubscribe(UUID subscriptionId) {
        ActiveSubscription subscription = subscriptions.remove(Objects.requireNonNull(subscriptionId, "subscriptionId"));
        if (subscription != null) {
            ScheduledFuture<?> future = subscription.future().get();
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    @Override
    public List<SubscriptionSnapshot> subscriptions() {
        return subscriptions.values().stream()
                .sorted(Comparator.comparing(ActiveSubscription::createdAt))
                .map(subscription -> new SubscriptionSnapshot(
                        subscription.id(),
                        subscription.metricId(),
                        subscription.interval(),
                        subscription.createdAt(),
                        Optional.ofNullable(subscription.lastUpdateAt().get())
                ))
                .toList();
    }

    @Override
    public void close() {
        for (UUID subscriptionId : List.copyOf(subscriptions.keySet())) {
            unsubscribe(subscriptionId);
        }
        executorService.shutdownNow();
        dashboardProvider.close();
    }

    private void publish(ActiveSubscription subscription, WeatherUpdateListener listener) {
        try {
            MetricValue value = currentValue(subscription.metricId());
            subscription.lastUpdateAt().set(value.measuredAt());
            listener.onUpdate(subscription.id(), value);
        } catch (RuntimeException exception) {
            listener.onError(subscription.id(), exception);
        }
    }

    private String normalizeMetricId(String metricId) {
        String normalized = Objects.requireNonNull(metricId, "metricId").trim();
        metric(normalized);
        return normalized;
    }

    private DataMetric metric(String metricId) {
        return supportedMetrics().stream()
                .filter(metric -> metric.id().equals(metricId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported metric id: " + metricId));
    }

    private Duration normalizedInterval(Duration interval) {
        Duration candidate = interval == null || interval.isNegative() || interval.isZero()
                ? MINIMUM_SUBSCRIPTION_INTERVAL
                : interval;
        return candidate.compareTo(MINIMUM_SUBSCRIPTION_INTERVAL) < 0 ? MINIMUM_SUBSCRIPTION_INTERVAL : candidate;
    }

    private record ActiveSubscription(
            UUID id,
            String metricId,
            Duration interval,
            Instant createdAt,
            AtomicReference<Instant> lastUpdateAt,
            AtomicReference<ScheduledFuture<?>> future
    ) {
        private ActiveSubscription(UUID id, String metricId, Duration interval, Instant createdAt) {
            this(id, metricId, interval, createdAt, new AtomicReference<>(), new AtomicReference<>());
        }
    }

    private static final class ThreadIds {
        private static final AtomicInteger NEXT = new AtomicInteger();

        private ThreadIds() {
        }
    }
}
