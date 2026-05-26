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
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Local simulated provider implementing the same port expected from future remote providers.
 */
public final class SimulatedWeatherDataProvider implements WeatherDataProvider {
    private static final ProviderDescriptor DESCRIPTOR = new ProviderDescriptor(
            "Aelia Local Simulator",
            "0.2.0",
            "Realistic local values with no remote API and no API key.",
            false,
            "Free local simulation; configurable replacement point for remote providers.",
            Optional.of(URI.create("https://github.com/alescis-wuin/Aelia/tree/develop/docs/V0.2"))
    );

    private static final int MAX_SUBSCRIPTIONS = 8;

    private final Map<String, DataMetric> metricsById = SimulationCatalog.metricsById();
    private final Map<String, MetricState> numericStates = new ConcurrentHashMap<>();
    private final WeatherConditionState conditionState;
    private final SunCycleState sunCycleState;
    private final ScheduledExecutorService executorService;
    private final Map<UUID, SubscriptionTask> subscriptionTasks = new ConcurrentHashMap<>();

    public SimulatedWeatherDataProvider() {
        Instant now = Instant.now();
        for (MetricProfile profile : SimulationCatalog.profiles()) {
            numericStates.put(profile.metric().id(), new MetricState(profile, profile.metric().id().hashCode(), now));
        }
        this.conditionState = new WeatherConditionState(metric("condition"), 2_026L, now);
        this.sunCycleState = new SunCycleState(metricsById, ZoneId.systemDefault());
        this.executorService = Executors.newScheduledThreadPool(2, daemonThreadFactory());
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
        DataMetric dataMetric = metric(metricId);
        String normalizedMetricId = dataMetric.id();
        Instant now = Instant.now();
        if (dataMetric.kind() == DataKind.NUMERIC) {
            MetricState state = numericStates.get(normalizedMetricId);
            if (state == null) {
                throw new IllegalArgumentException("unsupported numeric metric: " + metricId);
            }
            return state.sample(now);
        }
        if ("condition".equals(normalizedMetricId)) {
            return conditionState.sample(now);
        }
        if ("sunrise".equals(normalizedMetricId) || "sunset".equals(normalizedMetricId)) {
            return sunCycleState.sample(normalizedMetricId, now);
        }
        throw new IllegalArgumentException("unsupported metric: " + normalizedMetricId);
    }

    @Override
    public UUID subscribe(String metricId, Duration interval, WeatherUpdateListener listener) {
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(listener, "listener");
        DataMetric dataMetric = metric(metricId);
        if (subscriptionTasks.size() >= MAX_SUBSCRIPTIONS) {
            throw new IllegalStateException("maximum active subscriptions reached");
        }

        UUID subscriptionId = UUID.randomUUID();
        SubscriptionTask task = new SubscriptionTask(subscriptionId, dataMetric.id(), interval, Instant.now());
        Runnable command = () -> publish(subscriptionId, listener);
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                command,
                interval.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS
        );
        task.future().set(future);
        subscriptionTasks.put(subscriptionId, task);
        return subscriptionId;
    }

    @Override
    public void unsubscribe(UUID subscriptionId) {
        SubscriptionTask removed = subscriptionTasks.remove(Objects.requireNonNull(subscriptionId, "subscriptionId"));
        if (removed != null) {
            ScheduledFuture<?> future = removed.future().get();
            if (future != null) {
                future.cancel(false);
            }
        }
    }

    @Override
    public List<SubscriptionSnapshot> subscriptions() {
        return subscriptionTasks.values().stream()
                .map(SubscriptionTask::snapshot)
                .sorted(Comparator.comparing(SubscriptionSnapshot::createdAt))
                .toList();
    }

    @Override
    public void close() {
        for (UUID subscriptionId : List.copyOf(subscriptionTasks.keySet())) {
            unsubscribe(subscriptionId);
        }
        executorService.shutdownNow();
    }

    private void publish(UUID subscriptionId, WeatherUpdateListener listener) {
        SubscriptionTask task = subscriptionTasks.get(subscriptionId);
        if (task == null) {
            return;
        }
        try {
            MetricValue value = currentValue(task.metricId());
            task.markUpdated(value.timestamp());
            listener.onUpdate(subscriptionId, value);
        } catch (RuntimeException error) {
            listener.onError(subscriptionId, error);
        }
    }

    private DataMetric metric(String metricId) {
        String normalized = Objects.requireNonNull(metricId, "metricId").trim();
        DataMetric dataMetric = metricsById.get(normalized);
        if (dataMetric == null) {
            throw new IllegalArgumentException("unsupported metric: " + normalized);
        }
        return dataMetric;
    }

    private ThreadFactory daemonThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "aelia-simulator");
            thread.setDaemon(true);
            return thread;
        };
    }

    private record SubscriptionTask(
            UUID id,
            String metricId,
            Duration interval,
            Instant createdAt,
            AtomicReference<Instant> lastUpdateAt,
            AtomicReference<ScheduledFuture<?>> future
    ) {
        SubscriptionTask(UUID id, String metricId, Duration interval, Instant createdAt) {
            this(id, metricId, interval, createdAt, new AtomicReference<>(), new AtomicReference<>());
        }

        void markUpdated(Instant timestamp) {
            lastUpdateAt.set(timestamp);
        }

        SubscriptionSnapshot snapshot() {
            return new SubscriptionSnapshot(
                    id,
                    metricId,
                    interval,
                    createdAt,
                    Optional.ofNullable(lastUpdateAt.get())
            );
        }
    }
}
