package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.MetricReading;
import fr.alescis.aelia.model.SubscriptionHandle;
import fr.alescis.aelia.model.SubscriptionRequest;
import fr.alescis.aelia.model.WeatherMetric;
import fr.alescis.aelia.provider.WeatherDashboardProvider;
import fr.alescis.aelia.provider.WeatherUpdateListener;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Deterministic simulator that reproduces the mockup values while exposing the remote-provider contract.
 */
public final class SimulatedWeatherDashboardProvider implements WeatherDashboardProvider {
    private final ScheduledExecutorService executor;
    private final Map<String, MetricProfile> metricProfiles;
    private final Map<SubscriptionHandle, ScheduledFuture<?>> subscriptions = new ConcurrentHashMap<>();

    public SimulatedWeatherDashboardProvider() {
        this.metricProfiles = SimulationCatalog.metricProfiles();
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "aelia-simulated-weather");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public DashboardSnapshot currentSnapshot() {
        return new DashboardSnapshot(
                SimulationCatalog.locations(),
                SimulationCatalog.currentWeather(),
                SimulationCatalog.hourlyForecasts(),
                SimulationCatalog.dailyForecasts(),
                SimulationCatalog.pollenRisks(),
                supportedMetrics(),
                apiLimits()
        );
    }

    @Override
    public List<WeatherMetric> supportedMetrics() {
        return SimulationCatalog.metrics();
    }

    @Override
    public List<ApiLimit> apiLimits() {
        return SimulationCatalog.limits();
    }

    @Override
    public MetricReading currentValue(String metricId) {
        Objects.requireNonNull(metricId, "metricId");
        MetricProfile profile = metricProfiles.get(metricId);
        if (profile == null) {
            throw new IllegalArgumentException("Unsupported metric id: " + metricId);
        }
        double value = vary(profile.baseValue(), profile.amplitude(), profile.minimum(), profile.maximum());
        return new MetricReading(metricId, value, profile.unit(), Instant.now());
    }

    @Override
    public SubscriptionHandle subscribe(SubscriptionRequest request, WeatherUpdateListener listener) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");
        if (!metricProfiles.containsKey(request.metricId())) {
            throw new IllegalArgumentException("Unsupported metric id: " + request.metricId());
        }
        SubscriptionHandle handle = new SubscriptionHandle(UUID.randomUUID(), request.metricId());
        Runnable task = () -> listener.onUpdate(currentValue(request.metricId()));
        long intervalMillis = Math.max(1L, request.interval().toMillis());
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(task, 0L, intervalMillis, TimeUnit.MILLISECONDS);
        subscriptions.put(handle, future);
        return handle;
    }

    @Override
    public void unsubscribe(SubscriptionHandle handle) {
        ScheduledFuture<?> future = subscriptions.remove(handle);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    public void close() {
        for (ScheduledFuture<?> future : subscriptions.values()) {
            future.cancel(false);
        }
        subscriptions.clear();
        executor.shutdownNow();
    }

    private double vary(double baseValue, double amplitude, double minimum, double maximum) {
        double variation = ThreadLocalRandom.current().nextDouble(-amplitude, amplitude);
        return Math.max(minimum, Math.min(maximum, baseValue + variation));
    }
}
