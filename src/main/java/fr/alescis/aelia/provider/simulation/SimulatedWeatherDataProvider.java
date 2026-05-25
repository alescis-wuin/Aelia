package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DataKind;
import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.model.ProviderDescriptor;
import fr.alescis.aelia.model.SubscriptionSnapshot;
import fr.alescis.aelia.ports.WeatherDataProvider;
import fr.alescis.aelia.ports.WeatherUpdateListener;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

/**
 * Local provider that simulates realistic weather and environmental data.
 */
public final class SimulatedWeatherDataProvider implements WeatherDataProvider {

    private static final ProviderDescriptor DESCRIPTOR = new ProviderDescriptor(
            "simulated-weather-provider",
            "Local simulated weather provider",
            "0.1.0",
            "Local simulator; no network access and no API key.",
            Optional.of(URI.create("https://github.com/alescis-wuin/Aeliea/tree/develop/docs/V0.1"))
    );

    private final ScheduledExecutorService executorService;
    private final Map<String, MetricState> numericStates;
    private final WeatherConditionState conditionState;
    private final SunCycleState sunCycleState;
    private final Map<UUID, ActiveSubscription> subscriptions = new ConcurrentHashMap<>();

    public SimulatedWeatherDataProvider() {
        Instant now = Instant.now();
        this.executorService = Executors.newScheduledThreadPool(2, new SimulatorThreadFactory());
        this.numericStates = SimulationCatalog.numericProfiles().stream()
                .collect(Collectors.toConcurrentMap(
                        profile -> profile.metric().id(),
                        profile -> new MetricState(profile, new Random(profile.metric().id().hashCode() * 31L + now.getEpochSecond()), now)
                ));
        this.conditionState = new WeatherConditionState(new Random(7_331L + now.getEpochSecond()), now);
        this.sunCycleState = new SunCycleState(ZoneId.systemDefault());
    }

    @Override
    public ProviderDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public List<DataMetric> supportedMetrics() {
        return SimulationCatalog.metrics();
    }

    @Override
    public List<ApiLimit> limits() {
        return SimulationCatalog.limits();
    }

    @Override
    public MetricValue currentValue(String metricId) {
        Objects.requireNonNull(metricId, "metricId");
        DataMetric metric = SimulationCatalog.metricById(metricId);
        Instant now = Instant.now();
        if (metric.kind() == DataKind.NUMERIC) {
            MetricState state = numericStates.get(metricId);
            if (state == null) {
                throw new IllegalStateException("Missing numeric state for metric: " + metricId);
            }
            return state.sample(now);
        }
        if (metric.id().equals(SimulationCatalog.weatherConditionMetric().id())) {
            return conditionState.sample(now);
        }
        if (metric.id().equals(SimulationCatalog.sunriseMetric().id())) {
            return sunCycleState.sunrise(now);
        }
        if (metric.id().equals(SimulationCatalog.sunsetMetric().id())) {
            return sunCycleState.sunset(now);
        }
        throw new IllegalArgumentException("Unsupported metric: " + metricId);
    }

    @Override
    public UUID subscribe(String metricId, Duration interval, WeatherUpdateListener listener) {
        Objects.requireNonNull(metricId, "metricId");
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(listener, "listener");
        DataMetric metric = SimulationCatalog.metricById(metricId);
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Subscription interval must be positive.");
        }

        UUID id = UUID.randomUUID();
        Runnable task = () -> publishSafely(id, metric, listener);
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                task,
                Math.min(250L, interval.toMillis()),
                interval.toMillis(),
                TimeUnit.MILLISECONDS
        );
        subscriptions.put(id, new ActiveSubscription(id, metric, interval, Instant.now(), future));
        return id;
    }

    @Override
    public void unsubscribe(UUID subscriptionId) {
        Objects.requireNonNull(subscriptionId, "subscriptionId");
        ActiveSubscription subscription = subscriptions.remove(subscriptionId);
        if (subscription != null) {
            subscription.future().cancel(false);
        }
    }

    @Override
    public List<SubscriptionSnapshot> activeSubscriptions() {
        return subscriptions.values().stream()
                .sorted(Comparator.comparing(ActiveSubscription::createdAt))
                .map(subscription -> new SubscriptionSnapshot(
                        subscription.id(),
                        subscription.metric(),
                        subscription.interval(),
                        subscription.createdAt()
                ))
                .toList();
    }

    @Override
    public void close() {
        subscriptions.keySet().forEach(this::unsubscribe);
        executorService.shutdownNow();
    }

    private void publishSafely(UUID subscriptionId, DataMetric metric, WeatherUpdateListener listener) {
        try {
            listener.onUpdate(subscriptionId, currentValue(metric.id()));
        } catch (RuntimeException exception) {
            listener.onError(subscriptionId, exception);
        }
    }

    private record ActiveSubscription(
            UUID id,
            DataMetric metric,
            Duration interval,
            Instant createdAt,
            ScheduledFuture<?> future
    ) {
    }

    private static final class SimulatorThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "weather-simulator-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
