package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DataCategory;
import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.model.ProviderDescriptor;
import fr.alescis.aelia.model.SubscriptionSnapshot;
import fr.alescis.aelia.model.WeatherMetric;
import fr.alescis.aelia.ports.WeatherDataProvider;
import fr.alescis.aelia.ports.WeatherUpdateListener;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Local provider that exposes the simulated dashboard metrics through the generic data-provider port.
 */
public final class SimulatedWeatherDataProvider implements WeatherDataProvider {
    private static final ProviderDescriptor DESCRIPTOR = new ProviderDescriptor(
            "simulated-weather-provider",
            "Local simulated weather provider",
            "0.3.11",
            "Local simulator; no network access and no API key.",
            Optional.of(URI.create("https://github.com/alescis-wuin/Aelia/tree/develop/docs/V0.3.11"))
    );

    private final ScheduledExecutorService executorService;
    private final Map<String, MetricProfile> numericProfiles;
    private final Map<String, DataMetric> metricsById;
    private final SunCycleState sunCycleState;
    private final Map<UUID, ActiveSubscription> subscriptions = new ConcurrentHashMap<>();

    public SimulatedWeatherDataProvider() {
        this.executorService = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "aelia-data-simulator-" + ThreadIds.NEXT.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        this.numericProfiles = Map.copyOf(SimulationCatalog.metricProfiles());
        this.metricsById = buildMetricsById();
        this.sunCycleState = new SunCycleState(ZoneId.systemDefault());
    }

    @Override
    public ProviderDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public List<DataMetric> supportedMetrics() {
        return List.copyOf(metricsById.values());
    }

    @Override
    public List<ApiLimit> limits() {
        return SimulationCatalog.limits();
    }

    @Override
    public MetricValue currentValue(String metricId) {
        DataMetric metric = metricById(metricId);
        Instant now = Instant.now();
        if ("sunrise".equals(metric.id())) {
            return sunCycleState.sunrise(metric, now);
        }
        if ("sunset".equals(metric.id())) {
            return sunCycleState.sunset(metric, now);
        }
        MetricProfile profile = numericProfiles.get(metric.id());
        if (profile == null) {
            throw new IllegalArgumentException("Unsupported metric id: " + metricId);
        }
        double value = varied(profile);
        return MetricValue.numeric(metric, now, value);
    }

    @Override
    public UUID subscribe(String metricId, Duration interval, WeatherUpdateListener listener) {
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(listener, "listener");
        DataMetric metric = metricById(metricId);
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Subscription interval must be positive.");
        }
        UUID id = UUID.randomUUID();
        ActiveSubscription subscription = new ActiveSubscription(id, metric.id(), interval, Instant.now());
        Runnable task = () -> publishSafely(subscription, listener);
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                task,
                0L,
                Math.max(1L, interval.toMillis()),
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
    }

    private void publishSafely(ActiveSubscription subscription, WeatherUpdateListener listener) {
        try {
            MetricValue value = currentValue(subscription.metricId());
            subscription.lastUpdateAt().set(value.measuredAt());
            listener.onUpdate(subscription.id(), value);
        } catch (RuntimeException exception) {
            listener.onError(subscription.id(), exception);
        }
    }

    private DataMetric metricById(String metricId) {
        String normalized = Objects.requireNonNull(metricId, "metricId").trim();
        DataMetric metric = metricsById.get(normalized);
        if (metric == null) {
            throw new IllegalArgumentException("Unsupported metric id: " + normalized);
        }
        return metric;
    }

    private Map<String, DataMetric> buildMetricsById() {
        Map<String, DataMetric> metrics = new HashMap<>();
        for (WeatherMetric weatherMetric : SimulationCatalog.metrics()) {
            DataMetric metric = DataMetric.numeric(
                    weatherMetric.id(),
                    weatherMetric.label(),
                    categoryFor(weatherMetric),
                    weatherMetric.unit()
            );
            metrics.put(metric.id(), metric);
        }
        DataMetric sunrise = DataMetric.text("sunrise", "Lever du soleil", DataCategory.ASTRONOMY);
        DataMetric sunset = DataMetric.text("sunset", "Coucher du soleil", DataCategory.ASTRONOMY);
        metrics.put(sunrise.id(), sunrise);
        metrics.put(sunset.id(), sunset);
        return Map.copyOf(metrics);
    }

    private DataCategory categoryFor(WeatherMetric metric) {
        String category = metric.category().toLowerCase(java.util.Locale.ROOT);
        if (category.contains("air")) {
            return DataCategory.AIR_QUALITY;
        }
        if (category.contains("pollen")) {
            return DataCategory.POLLEN;
        }
        if (category.contains("sant")) {
            return DataCategory.HEALTH;
        }
        if (category.contains("atmos")) {
            return DataCategory.ATMOSPHERE;
        }
        return DataCategory.WEATHER;
    }

    private double varied(MetricProfile profile) {
        double variation = ThreadLocalRandom.current().nextDouble(-profile.amplitude(), profile.amplitude());
        return Math.max(profile.minimum(), Math.min(profile.maximum(), profile.baseValue() + variation));
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
