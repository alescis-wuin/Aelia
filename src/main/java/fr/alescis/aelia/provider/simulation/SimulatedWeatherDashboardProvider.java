package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.AirQuality;
import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.CurrentWeather;
import fr.alescis.aelia.model.DailyForecast;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.HourlyForecast;
import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.model.MetricReading;
import fr.alescis.aelia.model.PollenLevel;
import fr.alescis.aelia.model.PollenRisk;
import fr.alescis.aelia.model.SubscriptionHandle;
import fr.alescis.aelia.model.SubscriptionRequest;
import fr.alescis.aelia.model.WeatherCondition;
import fr.alescis.aelia.model.WeatherMetric;
import fr.alescis.aelia.provider.WeatherDashboardProvider;
import fr.alescis.aelia.provider.WeatherUpdateListener;

import java.time.Instant;
import java.util.ArrayList;
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

    private static final List<WeatherMetric> METRICS = List.of(
            new WeatherMetric("temperature", "Température", "Météo", "°C", "Actuel · Horaire · Quotidien", -20.0, 45.0),
            new WeatherMetric("humidity", "Humidité", "Atmosphère", "%", "Actuel · Horaire", 0.0, 100.0),
            new WeatherMetric("wind", "Vent", "Atmosphère", "km/h", "Actuel · Horaire · Quotidien", 0.0, 140.0),
            new WeatherMetric("pressure", "Pression", "Atmosphère", "hPa", "Actuel · Horaire", 940.0, 1060.0),
            new WeatherMetric("uv", "Indice UV", "Santé", "", "Actuel · Horaire · Quotidien", 0.0, 12.0),
            new WeatherMetric("aqi", "IQA", "Qualité de l'air", "", "Actuel · Horaire", 0.0, 500.0),
            new WeatherMetric("pm25", "PM2.5", "Qualité de l'air", "µg/m³", "Actuel · Horaire", 0.0, 250.0),
            new WeatherMetric("grass_pollen", "Pollen graminées", "Pollens", "niveau", "Actuel · Quotidien", 0.0, 4.0)
    );

    private static final List<ApiLimit> LIMITS = List.of(
            new ApiLimit("Current snapshot", "seconde", "10 requêtes"),
            new ApiLimit("Subscriptions", "client", "25 flux actifs"),
            new ApiLimit("Minimum interval", "flux", "1 seconde"),
            new ApiLimit("Remote calls", "réseau", "aucun en simulation")
    );

    private final ScheduledExecutorService executor;
    private final Map<SubscriptionHandle, ScheduledFuture<?>> subscriptions = new ConcurrentHashMap<>();

    public SimulatedWeatherDashboardProvider() {
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "aelia-simulated-weather");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public DashboardSnapshot currentSnapshot() {
        return new DashboardSnapshot(
                locations(),
                currentWeather(),
                hourlyForecasts(),
                dailyForecasts(),
                pollenRisks(),
                supportedMetrics(),
                apiLimits()
        );
    }

    @Override
    public List<WeatherMetric> supportedMetrics() {
        return METRICS;
    }

    @Override
    public List<ApiLimit> apiLimits() {
        return LIMITS;
    }

    @Override
    public MetricReading currentValue(String metricId) {
        Objects.requireNonNull(metricId, "metricId");
        CurrentWeather weather = currentWeather();
        double value = switch (metricId) {
            case "temperature" -> vary(weather.temperatureCelsius(), 0.35, -20.0, 45.0);
            case "humidity" -> vary(weather.humidityPercent(), 1.25, 0.0, 100.0);
            case "wind" -> vary(weather.windSpeedKmh(), 1.5, 0.0, 140.0);
            case "pressure" -> vary(weather.pressureHpa(), 0.8, 940.0, 1060.0);
            case "uv" -> vary(weather.uvIndex(), 0.2, 0.0, 12.0);
            case "aqi" -> vary(weather.airQuality().airQualityIndex(), 1.4, 0.0, 500.0);
            case "pm25" -> vary(weather.airQuality().pm25MicrogramsPerCubicMeter(), 0.8, 0.0, 250.0);
            case "grass_pollen" -> 3.0;
            default -> throw new IllegalArgumentException("Unsupported metric id: " + metricId);
        };
        String unit = METRICS.stream()
                .filter(metric -> metric.id().equals(metricId))
                .findFirst()
                .map(WeatherMetric::unit)
                .orElse("");
        return new MetricReading(metricId, value, unit, Instant.now());
    }

    @Override
    public SubscriptionHandle subscribe(SubscriptionRequest request, WeatherUpdateListener listener) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");
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

    private List<LocationWeather> locations() {
        return List.of(
                new LocationWeather("Paris", "France", WeatherCondition.SUNNY, 24, true),
                new LocationWeather("Tokyo", "Japon", WeatherCondition.PARTLY_CLOUDY, 19, false),
                new LocationWeather("New York", "USA", WeatherCondition.RAINY, 11, false),
                new LocationWeather("Dubaï", "EAU", WeatherCondition.SUNNY, 38, false)
        );
    }

    private CurrentWeather currentWeather() {
        return new CurrentWeather(
                "Paris",
                "Ensoleillé",
                "Dim. 25 mai 2025",
                24,
                28,
                17,
                22,
                "06:04",
                "21:47",
                62,
                18,
                "SO → NE",
                32,
                1018,
                "↗ En hausse",
                5,
                "Protection solaire recommandée entre 11h et 16h",
                new AirQuality(42, "BON", 12, 28, 18),
                "15h 43 min de jour",
                "~15h30"
        );
    }

    private List<HourlyForecast> hourlyForecasts() {
        return List.of(
                new HourlyForecast("9h", WeatherCondition.SUNNY, 18, false),
                new HourlyForecast("12h", WeatherCondition.SUNNY, 24, true),
                new HourlyForecast("15h", WeatherCondition.SUNNY, 27, false),
                new HourlyForecast("18h", WeatherCondition.PARTLY_CLOUDY, 23, false),
                new HourlyForecast("21h", WeatherCondition.CLEAR_NIGHT, 19, false),
                new HourlyForecast("00h", WeatherCondition.CLEAR_NIGHT, 16, false),
                new HourlyForecast("03h", WeatherCondition.CLEAR_NIGHT, 14, false),
                new HourlyForecast("06h", WeatherCondition.SUNNY, 15, false),
                new HourlyForecast("09h", WeatherCondition.SUNNY, 20, false)
        );
    }

    private List<DailyForecast> dailyForecasts() {
        return List.of(
                new DailyForecast("Lun. 26", WeatherCondition.SUNNY, 26, 15, 5, 14, 4, true),
                new DailyForecast("Mar. 27", WeatherCondition.PARTLY_CLOUDY, 22, 14, 40, 22, 3, false),
                new DailyForecast("Mer. 28", WeatherCondition.RAINY, 19, 13, 75, 28, 2, false),
                new DailyForecast("Jeu. 29", WeatherCondition.SUNNY, 27, 16, 0, 16, 6, false),
                new DailyForecast("Ven. 30", WeatherCondition.SUNNY, 29, 18, 0, 12, 7, false),
                new DailyForecast("Sam. 31", WeatherCondition.SUNNY, 26, 15, 10, 15, 5, false),
                new DailyForecast("Dim. 01", WeatherCondition.SUNNY, 28, 17, 0, 18, 5, false)
        );
    }

    private List<PollenRisk> pollenRisks() {
        return new ArrayList<>(List.of(
                new PollenRisk("Graminées", PollenLevel.HIGH),
                new PollenRisk("Bouleau", PollenLevel.MODERATE),
                new PollenRisk("Olivier", PollenLevel.LOW),
                new PollenRisk("Ambroisie", PollenLevel.NONE)
        ));
    }

    private double vary(double baseValue, double amplitude, double minimum, double maximum) {
        double variation = ThreadLocalRandom.current().nextDouble(-amplitude, amplitude);
        return Math.max(minimum, Math.min(maximum, baseValue + variation));
    }
}
