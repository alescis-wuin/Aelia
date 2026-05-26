package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DashboardDataStatus;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.MetricReading;
import fr.alescis.aelia.model.SubscriptionHandle;
import fr.alescis.aelia.model.SubscriptionRequest;
import fr.alescis.aelia.model.WeatherMetric;
import fr.alescis.aelia.provider.ProviderDiagnostics;
import fr.alescis.aelia.provider.WeatherDashboardProvider;

import java.time.Duration;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Open-Meteo backed provider with a multi-source fallback chain.
 */
public final class OpenMeteoWeatherDashboardProvider implements WeatherDashboardProvider {
    private static final Duration MINIMUM_SUBSCRIPTION_INTERVAL = Duration.ofSeconds(30);

    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoHttpClient httpClient;
    private final OpenMeteoForecastClient forecastClient;
    private final WeatherApiComForecastClient weatherApiClient;
    private final VisualCrossingForecastClient visualCrossingClient;
    private final OpenWeatherForecastClient openWeatherClient;
    private final PirateWeatherForecastClient pirateWeatherClient;
    private final WeatherbitForecastClient weatherbitClient;
    private final MetNorwayForecastClient metNorwayClient;
    private final OpenMeteoArchiveClient archiveClient;
    private final OpenMeteoAirQualityClient airQualityClient;
    private final OpenMeteoMapper mapper = new OpenMeteoMapper();
    private final WeatherApiComFallbackMapper weatherApiMapper = new WeatherApiComFallbackMapper();
    private final VisualCrossingForecastFallbackMapper visualCrossingMapper = new VisualCrossingForecastFallbackMapper();
    private final OpenWeatherForecastFallbackMapper openWeatherMapper = new OpenWeatherForecastFallbackMapper();
    private final PirateWeatherForecastFallbackMapper pirateWeatherMapper = new PirateWeatherForecastFallbackMapper();
    private final WeatherbitForecastFallbackMapper weatherbitMapper = new WeatherbitForecastFallbackMapper();
    private final MetNorwayForecastFallbackMapper metNorwayMapper = new MetNorwayForecastFallbackMapper();
    private final OpenMeteoArchiveFallbackMapper archiveFallbackMapper = new OpenMeteoArchiveFallbackMapper();
    private final ScheduledExecutorService executorService;
    private final Map<SubscriptionHandle, ScheduledFuture<?>> dashboardSubscriptions = new ConcurrentHashMap<>();
    private volatile DashboardSnapshot lastSnapshot;

    public OpenMeteoWeatherDashboardProvider() {
        this(OpenMeteoConfiguration.fromSystemProperties());
    }

    public OpenMeteoWeatherDashboardProvider(OpenMeteoConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = new OpenMeteoHttpClient(configuration);
        this.forecastClient = new OpenMeteoForecastClient(configuration, httpClient);
        this.weatherApiClient = new WeatherApiComForecastClient(configuration, httpClient);
        this.visualCrossingClient = new VisualCrossingForecastClient(configuration, httpClient);
        this.openWeatherClient = new OpenWeatherForecastClient(configuration, httpClient);
        this.pirateWeatherClient = new PirateWeatherForecastClient(configuration, httpClient);
        this.weatherbitClient = new WeatherbitForecastClient(configuration, httpClient);
        this.metNorwayClient = new MetNorwayForecastClient(configuration, httpClient);
        this.archiveClient = new OpenMeteoArchiveClient(configuration, httpClient);
        this.airQualityClient = new OpenMeteoAirQualityClient(configuration, httpClient);
        this.executorService = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "aelia-openmeteo-dashboard-" + ThreadIds.NEXT.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public DashboardSnapshot currentSnapshot() {
        OpenMeteoLocation selectedLocation = configuration.selectedLocation();
        ProviderDiagnostics.info("Loading remote weather snapshot for "
                + selectedLocation.city() + " (" + selectedLocation.latitude() + ", " + selectedLocation.longitude() + ").");
        try {
            DashboardSnapshot snapshot = forecastSnapshot(selectedLocation);
            lastSnapshot = snapshot;
            ProviderDiagnostics.info("Open-Meteo forecast snapshot mapped for "
                    + snapshot.currentWeather().city() + " with " + snapshot.hourlyForecasts().size()
                    + " hourly points and " + snapshot.dailyForecasts().size() + " daily points.");
            return snapshot;
        } catch (RuntimeException forecastException) {
            ProviderDiagnostics.warn("Open-Meteo Forecast API failed. Trying secondary forecast providers before Historical Weather API.", forecastException);
            DashboardSnapshot snapshot = fallbackSnapshot(selectedLocation, forecastException);
            lastSnapshot = snapshot;
            return snapshot;
        }
    }

    @Override
    public List<WeatherMetric> supportedMetrics() {
        return OpenMeteoMetricCatalog.weatherMetrics();
    }

    @Override
    public List<ApiLimit> apiLimits() {
        return OpenMeteoMetricCatalog.limits();
    }

    @Override
    public MetricReading currentValue(String metricId) {
        String normalizedMetricId = normalizeMetricId(metricId);
        DashboardSnapshot snapshot = cachedOrCurrentSnapshot();
        double value = mapper.metricValue(normalizedMetricId, snapshot);
        String unit = supportedMetrics().stream()
                .filter(metric -> metric.id().equals(normalizedMetricId))
                .map(WeatherMetric::unit)
                .findFirst()
                .orElse("");
        return new MetricReading(normalizedMetricId, value, unit, Instant.now());
    }

    @Override
    public SubscriptionHandle subscribe(SubscriptionRequest request, fr.alescis.aelia.provider.WeatherUpdateListener listener) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");
        String metricId = normalizeMetricId(request.metricId());
        SubscriptionHandle handle = new SubscriptionHandle(UUID.randomUUID(), metricId);
        Runnable task = () -> listener.onUpdate(currentValue(metricId));
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                task,
                0L,
                normalizedInterval(request.interval()).toMillis(),
                TimeUnit.MILLISECONDS
        );
        dashboardSubscriptions.put(handle, future);
        return handle;
    }

    @Override
    public void unsubscribe(SubscriptionHandle handle) {
        ScheduledFuture<?> future = dashboardSubscriptions.remove(Objects.requireNonNull(handle, "handle"));
        if (future != null) {
            future.cancel(false);
        }
    }

    DashboardSnapshot cachedOrCurrentSnapshot() {
        DashboardSnapshot snapshot = lastSnapshot;
        return snapshot == null ? currentSnapshot() : snapshot;
    }

    @Override
    public void close() {
        for (SubscriptionHandle handle : List.copyOf(dashboardSubscriptions.keySet())) {
            unsubscribe(handle);
        }
        executorService.shutdownNow();
        httpClient.close();
    }

    private DashboardSnapshot fallbackSnapshot(OpenMeteoLocation selectedLocation, RuntimeException forecastException) {
        List<RuntimeException> previousFailures = new ArrayList<>();
        previousFailures.add(forecastException);
        try {
            return weatherApiFallbackSnapshot(selectedLocation, forecastException);
        } catch (RuntimeException weatherApiException) {
            previousFailures.add(weatherApiException);
            ProviderDiagnostics.warn("WeatherAPI.com fallback is unavailable. Trying Visual Crossing Timeline Weather API.", weatherApiException);
        }
        try {
            return visualCrossingFallbackSnapshot(selectedLocation, forecastException);
        } catch (RuntimeException visualCrossingException) {
            previousFailures.add(visualCrossingException);
            ProviderDiagnostics.warn("Visual Crossing fallback is unavailable. Trying OpenWeather current and 5-day forecast APIs.", visualCrossingException);
        }
        try {
            return openWeatherFallbackSnapshot(selectedLocation, forecastException);
        } catch (RuntimeException openWeatherException) {
            previousFailures.add(openWeatherException);
            ProviderDiagnostics.warn("OpenWeather fallback is unavailable. Trying Pirate Weather.", openWeatherException);
        }
        try {
            return pirateWeatherFallbackSnapshot(selectedLocation, forecastException);
        } catch (RuntimeException pirateWeatherException) {
            previousFailures.add(pirateWeatherException);
            ProviderDiagnostics.warn("Pirate Weather fallback is unavailable. Trying Weatherbit.", pirateWeatherException);
        }
        try {
            return weatherbitFallbackSnapshot(selectedLocation, forecastException);
        } catch (RuntimeException weatherbitException) {
            previousFailures.add(weatherbitException);
            ProviderDiagnostics.warn("Weatherbit fallback is unavailable. Trying MET Norway Locationforecast.", weatherbitException);
        }
        try {
            return metNorwayFallbackSnapshot(selectedLocation, forecastException);
        } catch (RuntimeException metNorwayException) {
            previousFailures.add(metNorwayException);
            ProviderDiagnostics.warn("MET Norway Locationforecast fallback is unavailable. Trying Open-Meteo Historical Weather API.", metNorwayException);
        }
        try {
            return archiveFallbackSnapshot(selectedLocation, forecastException);
        } catch (RuntimeException archiveException) {
            for (RuntimeException failure : previousFailures) {
                archiveException.addSuppressed(failure);
            }
            throw archiveException;
        }
    }

    private DashboardSnapshot forecastSnapshot(OpenMeteoLocation selectedLocation) {
        OpenMeteoJsonValue forecast = forecastClient.fetchFullForecast(selectedLocation);
        OpenMeteoJsonValue airQuality = fetchOptionalAirQuality(selectedLocation);
        List<fr.alescis.aelia.model.LocationWeather> locations = locationSummaries(selectedLocation, forecast);
        return mapper.toDashboardSnapshot(
                selectedLocation,
                forecast,
                airQuality,
                locations,
                supportedMetrics(),
                apiLimits(),
                DashboardDataStatus.forecast("openmeteo")
        );
    }

    private DashboardSnapshot weatherApiFallbackSnapshot(OpenMeteoLocation selectedLocation, RuntimeException forecastException) {
        OpenMeteoJsonValue weatherApi = weatherApiClient.fetchForecast(selectedLocation);
        OpenMeteoJsonValue forecastLike = weatherApiMapper.toForecastLikePayload(selectedLocation, weatherApi);
        OpenMeteoJsonValue airQualityLike = weatherApiMapper.toAirQualityLikePayload(selectedLocation, weatherApi);
        List<fr.alescis.aelia.model.LocationWeather> locations = locationSummaries(selectedLocation, forecastLike);
        DashboardSnapshot snapshot = mapper.toDashboardSnapshot(
                selectedLocation,
                forecastLike,
                airQualityLike,
                locations,
                supportedMetrics(),
                apiLimits(),
                DashboardDataStatus.secondaryForecastFallback("openmeteo", "WeatherAPI.com Forecast", compactFailure(forecastException))
        );
        ProviderDiagnostics.warn("WeatherAPI.com fallback was used because Open-Meteo Forecast is unavailable.", null);
        return snapshot;
    }

    private DashboardSnapshot visualCrossingFallbackSnapshot(OpenMeteoLocation selectedLocation, RuntimeException forecastException) {
        OpenMeteoJsonValue visualCrossing = visualCrossingClient.fetchForecast(selectedLocation);
        OpenMeteoJsonValue forecastLike = visualCrossingMapper.toForecastLikePayload(selectedLocation, visualCrossing);
        List<fr.alescis.aelia.model.LocationWeather> locations = locationSummaries(selectedLocation, forecastLike);
        DashboardSnapshot snapshot = mapper.toDashboardSnapshot(
                selectedLocation,
                forecastLike,
                OpenMeteoJsonValue.missing(),
                locations,
                supportedMetrics(),
                apiLimits(),
                DashboardDataStatus.secondaryForecastFallback("openmeteo", "Visual Crossing Timeline Weather API", compactFailure(forecastException))
        );
        ProviderDiagnostics.warn("Visual Crossing fallback was used because Open-Meteo Forecast is unavailable.", null);
        return snapshot;
    }

    private DashboardSnapshot openWeatherFallbackSnapshot(OpenMeteoLocation selectedLocation, RuntimeException forecastException) {
        OpenMeteoJsonValue openWeather = openWeatherClient.fetchForecast(selectedLocation);
        OpenMeteoJsonValue forecastLike = openWeatherMapper.toForecastLikePayload(selectedLocation, openWeather);
        OpenMeteoJsonValue airQuality = fetchOptionalAirQuality(selectedLocation);
        List<fr.alescis.aelia.model.LocationWeather> locations = locationSummaries(selectedLocation, forecastLike);
        DashboardSnapshot snapshot = mapper.toDashboardSnapshot(
                selectedLocation,
                forecastLike,
                airQuality,
                locations,
                supportedMetrics(),
                apiLimits(),
                DashboardDataStatus.secondaryForecastFallback("openmeteo", "OpenWeather current + 5-day forecast", compactFailure(forecastException))
        );
        ProviderDiagnostics.warn("OpenWeather fallback was used because Open-Meteo Forecast is unavailable.", null);
        return snapshot;
    }

    private DashboardSnapshot pirateWeatherFallbackSnapshot(OpenMeteoLocation selectedLocation, RuntimeException forecastException) {
        OpenMeteoJsonValue pirateWeather = pirateWeatherClient.fetchForecast(selectedLocation);
        OpenMeteoJsonValue forecastLike = pirateWeatherMapper.toForecastLikePayload(selectedLocation, pirateWeather);
        List<fr.alescis.aelia.model.LocationWeather> locations = locationSummaries(selectedLocation, forecastLike);
        DashboardSnapshot snapshot = mapper.toDashboardSnapshot(
                selectedLocation,
                forecastLike,
                OpenMeteoJsonValue.missing(),
                locations,
                supportedMetrics(),
                apiLimits(),
                DashboardDataStatus.secondaryForecastFallback("openmeteo", "Pirate Weather Forecast", compactFailure(forecastException))
        );
        ProviderDiagnostics.warn("Pirate Weather fallback was used because Open-Meteo Forecast is unavailable.", null);
        return snapshot;
    }

    private DashboardSnapshot weatherbitFallbackSnapshot(OpenMeteoLocation selectedLocation, RuntimeException forecastException) {
        OpenMeteoJsonValue weatherbit = weatherbitClient.fetchForecast(selectedLocation);
        OpenMeteoJsonValue forecastLike = weatherbitMapper.toForecastLikePayload(selectedLocation, weatherbit);
        List<fr.alescis.aelia.model.LocationWeather> locations = locationSummaries(selectedLocation, forecastLike);
        DashboardSnapshot snapshot = mapper.toDashboardSnapshot(
                selectedLocation,
                forecastLike,
                OpenMeteoJsonValue.missing(),
                locations,
                supportedMetrics(),
                apiLimits(),
                DashboardDataStatus.secondaryForecastFallback("openmeteo", "Weatherbit Current + Daily Forecast", compactFailure(forecastException))
        );
        ProviderDiagnostics.warn("Weatherbit fallback was used because Open-Meteo Forecast is unavailable.", null);
        return snapshot;
    }

    private DashboardSnapshot metNorwayFallbackSnapshot(OpenMeteoLocation selectedLocation, RuntimeException forecastException) {
        OpenMeteoJsonValue metNorway = metNorwayClient.fetchForecast(selectedLocation);
        OpenMeteoJsonValue forecastLike = metNorwayMapper.toForecastLikePayload(selectedLocation, metNorway);
        OpenMeteoJsonValue airQuality = fetchOptionalAirQuality(selectedLocation);
        List<fr.alescis.aelia.model.LocationWeather> locations = locationSummaries(selectedLocation, forecastLike);
        DashboardSnapshot snapshot = mapper.toDashboardSnapshot(
                selectedLocation,
                forecastLike,
                airQuality,
                locations,
                supportedMetrics(),
                apiLimits(),
                DashboardDataStatus.secondaryForecastFallback("openmeteo", "MET Norway Locationforecast", compactFailure(forecastException))
        );
        ProviderDiagnostics.warn("MET Norway Locationforecast fallback was used because Open-Meteo Forecast is unavailable.", null);
        return snapshot;
    }

    private DashboardSnapshot archiveFallbackSnapshot(OpenMeteoLocation selectedLocation, RuntimeException forecastException) {
        try {
            OpenMeteoJsonValue archive = archiveClient.fetchArchiveFallback(selectedLocation);
            OpenMeteoJsonValue forecastLikeArchive = archiveFallbackMapper.toForecastLikePayload(selectedLocation, archive);
            List<fr.alescis.aelia.model.LocationWeather> locations = locationSummaries(selectedLocation, forecastLikeArchive);
            DashboardSnapshot snapshot = mapper.toDashboardSnapshot(
                    selectedLocation,
                    forecastLikeArchive,
                    OpenMeteoJsonValue.missing(),
                    locations,
                    supportedMetrics(),
                    apiLimits(),
                    DashboardDataStatus.archiveFallback("openmeteo", compactFailure(forecastException))
            );
            ProviderDiagnostics.warn("Historical Weather API fallback was used. Forecast-only cards are marked as unavailable.", null);
            return snapshot;
        } catch (RuntimeException archiveException) {
            archiveException.addSuppressed(forecastException);
            throw archiveException;
        }
    }

    private OpenMeteoJsonValue fetchOptionalAirQuality(OpenMeteoLocation selectedLocation) {
        try {
            return airQualityClient.fetchAirQuality(selectedLocation);
        } catch (RuntimeException exception) {
            ProviderDiagnostics.warn(
                    "Open-Meteo air-quality request failed. Weather values will still be displayed with unavailable air/pollen values.",
                    exception
            );
            return OpenMeteoJsonValue.missing();
        }
    }

    private List<fr.alescis.aelia.model.LocationWeather> locationSummaries(OpenMeteoLocation selectedLocation, OpenMeteoJsonValue selectedForecast) {
        List<fr.alescis.aelia.model.LocationWeather> summaries = new ArrayList<>();
        for (OpenMeteoLocation location : configuration.locations()) {
            boolean selected = sameCoordinates(location, selectedLocation);
            if (selected) {
                summaries.add(mapper.toLocationWeather(location, selectedForecast, true));
            } else {
                summaries.add(location.fallbackSummary(false));
            }
        }
        return List.copyOf(summaries);
    }

    private String normalizeMetricId(String metricId) {
        String normalized = Objects.requireNonNull(metricId, "metricId").trim();
        boolean known = OpenMeteoMetricCatalog.dataMetrics().stream().anyMatch(metric -> metric.id().equals(normalized));
        if (!known) {
            throw new IllegalArgumentException("Unsupported metric id: " + normalized);
        }
        return normalized;
    }

    private Duration normalizedInterval(Duration interval) {
        Duration candidate = interval == null || interval.isNegative() || interval.isZero()
                ? MINIMUM_SUBSCRIPTION_INTERVAL
                : interval;
        return candidate.compareTo(MINIMUM_SUBSCRIPTION_INTERVAL) < 0 ? MINIMUM_SUBSCRIPTION_INTERVAL : candidate;
    }

    private boolean sameCoordinates(OpenMeteoLocation left, OpenMeteoLocation right) {
        return Math.abs(left.latitude() - right.latitude()) < 0.0001
                && Math.abs(left.longitude() - right.longitude()) < 0.0001;
    }

    private String compactFailure(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String oneLine = message.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 180 ? oneLine.substring(0, 177) + "..." : oneLine;
    }

    private static final class ThreadIds {
        private static final AtomicInteger NEXT = new AtomicInteger();

        private ThreadIds() {
        }
    }
}
