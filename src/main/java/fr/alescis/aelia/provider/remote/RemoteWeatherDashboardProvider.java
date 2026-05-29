package fr.alescis.aelia.provider.remote;

import fr.alescis.aelia.model.AirQuality;
import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.CurrentWeather;
import fr.alescis.aelia.model.DailyForecast;
import fr.alescis.aelia.model.DashboardDataStatus;
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
import fr.alescis.aelia.provider.EnvironmentSettings;
import fr.alescis.aelia.provider.ProviderDiagnostics;
import fr.alescis.aelia.provider.WeatherDashboardProvider;
import fr.alescis.aelia.provider.WeatherProviderFactory;
import fr.alescis.aelia.provider.WeatherUpdateListener;
import fr.alescis.aelia.provider.simulation.SimulationCatalog;
import fr.alescis.aelia.provider.simulation.SimulatedWeatherDashboardProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Remote dashboard provider with Open-Meteo as the primary source and optional keyed fallbacks.
 */
public final class RemoteWeatherDashboardProvider implements WeatherDashboardProvider {
    private static final Duration MINIMUM_SUBSCRIPTION_INTERVAL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(15);

    private final String providerMode;
    private final RemoteConfiguration configuration;
    private final HttpClient httpClient;
    private final ScheduledExecutorService executorService;
    private final Map<URI, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private final Map<SubscriptionHandle, ScheduledFuture<?>> subscriptions = new ConcurrentHashMap<>();
    private volatile DashboardSnapshot lastSnapshot;

    public RemoteWeatherDashboardProvider(String providerMode) {
        this.providerMode = WeatherProviderFactory.normalizeMode(providerMode);
        this.configuration = RemoteConfiguration.fromRuntime();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(configuration.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.executorService = Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "aelia-remote-weather");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public DashboardSnapshot currentSnapshot() {
        List<RuntimeException> failures = new ArrayList<>();
        for (RemoteSource source : sources()) {
            if (!source.enabled()) {
                ProviderDiagnostics.info("Remote provider skipped: " + source.label() + " is disabled or not configured.");
                continue;
            }
            try {
                ProviderDiagnostics.info("Loading remote weather snapshot from " + source.label()
                        + " for " + configuration.city() + " (" + configuration.latitude() + ", " + configuration.longitude() + ").");
                DashboardSnapshot snapshot = source.load();
                lastSnapshot = snapshot;
                ProviderDiagnostics.info("Remote weather snapshot mapped from " + snapshot.dataStatus().sourceLabel()
                        + " for " + snapshot.currentWeather().city() + ".");
                return snapshot;
            } catch (RuntimeException exception) {
                failures.add(exception);
                ProviderDiagnostics.warn(source.label() + " failed. Trying next configured provider.", exception);
            }
        }
        RuntimeException failure = combinedFailure(failures);
        if (WeatherProviderFactory.MODE_AUTO.equals(providerMode)) {
            ProviderDiagnostics.warn("Every remote provider failed in auto mode. Falling back to simulation.", failure);
            return simulatedFallback(failure);
        }
        throw failure;
    }

    @Override
    public List<WeatherMetric> supportedMetrics() {
        return SimulationCatalog.metrics();
    }

    @Override
    public List<ApiLimit> apiLimits() {
        return List.of(
                new ApiLimit("Open-Meteo Forecast", "cache", "15 min"),
                new ApiLimit("Open-Meteo Air Quality", "cache", "45 min"),
                new ApiLimit("WeatherAPI.com fallback", "clé", configuration.weatherApiEnabled() ? "activé" : "désactivé"),
                new ApiLimit("Visual Crossing fallback", "clé", configuration.visualCrossingEnabled() ? "activé" : "désactivé"),
                new ApiLimit("OpenWeather fallback", "clé", configuration.openWeatherEnabled() ? "activé" : "désactivé"),
                new ApiLimit("Weatherbit fallback", "clé", configuration.weatherbitEnabled() ? "activé" : "désactivé"),
                new ApiLimit("Pirate Weather fallback", "clé", configuration.pirateWeatherEnabled() ? "activé" : "désactivé")
        );
    }

    @Override
    public MetricReading currentValue(String metricId) {
        Objects.requireNonNull(metricId, "metricId");
        DashboardSnapshot snapshot = cachedOrCurrentSnapshot();
        String normalized = metricId.trim().toLowerCase(Locale.ROOT);
        CurrentWeather weather = snapshot.currentWeather();
        double value = switch (normalized) {
            case "temperature" -> weather.temperatureCelsius();
            case "humidity" -> weather.humidityPercent();
            case "wind" -> weather.windSpeedKmh();
            case "pressure" -> weather.pressureHpa();
            case "uv" -> weather.uvIndex();
            case "aqi" -> weather.airQuality().airQualityIndex();
            case "pm25" -> weather.airQuality().pm25MicrogramsPerCubicMeter();
            case "grass_pollen" -> snapshot.pollenRisks().isEmpty() ? 0.0 : snapshot.pollenRisks().get(0).level().ordinal();
            default -> throw new IllegalArgumentException("Unsupported metric id: " + metricId);
        };
        String unit = supportedMetrics().stream()
                .filter(metric -> metric.id().equals(normalized))
                .map(WeatherMetric::unit)
                .findFirst()
                .orElse("");
        return new MetricReading(normalized, value, unit, Instant.now());
    }

    @Override
    public SubscriptionHandle subscribe(SubscriptionRequest request, WeatherUpdateListener listener) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");
        SubscriptionHandle handle = new SubscriptionHandle(UUID.randomUUID(), request.metricId());
        long intervalMillis = Math.max(MINIMUM_SUBSCRIPTION_INTERVAL.toMillis(), request.interval().toMillis());
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                () -> listener.onUpdate(currentValue(request.metricId())),
                0L,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );
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
        executorService.shutdownNow();
    }

    private DashboardSnapshot cachedOrCurrentSnapshot() {
        DashboardSnapshot snapshot = lastSnapshot;
        return snapshot == null ? currentSnapshot() : snapshot;
    }

    private List<RemoteSource> sources() {
        return List.of(
                new RemoteSource("Open-Meteo Forecast", true, this::openMeteoSnapshot),
                new RemoteSource("WeatherAPI.com Forecast", configuration.weatherApiEnabled(), this::weatherApiSnapshot),
                new RemoteSource("Visual Crossing Timeline", configuration.visualCrossingEnabled(), this::visualCrossingSnapshot),
                new RemoteSource("OpenWeather", configuration.openWeatherEnabled(), this::openWeatherSnapshot),
                new RemoteSource("Weatherbit", configuration.weatherbitEnabled(), this::weatherbitSnapshot),
                new RemoteSource("Pirate Weather", configuration.pirateWeatherEnabled(), this::pirateWeatherSnapshot)
        );
    }

    private DashboardSnapshot openMeteoSnapshot() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("latitude", Double.toString(configuration.latitude()));
        query.put("longitude", Double.toString(configuration.longitude()));
        query.put("timezone", configuration.zoneId().getId());
        query.put("forecast_days", "7");
        query.put("current", String.join(",",
                "temperature_2m",
                "relative_humidity_2m",
                "apparent_temperature",
                "weather_code",
                "wind_speed_10m",
                "wind_direction_10m",
                "wind_gusts_10m",
                "surface_pressure"));
        query.put("hourly", "temperature_2m,weather_code");
        query.put("daily", String.join(",",
                "weather_code",
                "temperature_2m_max",
                "temperature_2m_min",
                "precipitation_probability_max",
                "wind_speed_10m_max",
                "uv_index_max",
                "sunrise",
                "sunset"));

        URI forecastUri = appendQuery(configuration.openMeteoForecastEndpoint(), query);
        String forecast = fetchText(forecastUri, configuration.forecastCacheTtl(), "Open-Meteo Forecast");

        String airQuality = "";
        try {
            Map<String, String> airQuery = new LinkedHashMap<>();
            airQuery.put("latitude", Double.toString(configuration.latitude()));
            airQuery.put("longitude", Double.toString(configuration.longitude()));
            airQuery.put("timezone", configuration.zoneId().getId());
            airQuery.put("forecast_days", "3");
            airQuery.put("current", "european_aqi,pm2_5,pm10,nitrogen_dioxide,grass_pollen,birch_pollen,olive_pollen,ragweed_pollen");
            airQuality = fetchText(appendQuery(configuration.openMeteoAirQualityEndpoint(), airQuery), configuration.airQualityCacheTtl(), "Open-Meteo Air Quality");
        } catch (RuntimeException exception) {
            ProviderDiagnostics.warn("Open-Meteo air-quality data unavailable; weather values will still be used.", exception);
        }

        RemoteReading reading = RemoteReading.fromOpenMeteo(configuration, forecast, airQuality);
        return reading.toSnapshot(DashboardDataStatus.forecast(providerMode).withProviderMode(providerMode));
    }

    private DashboardSnapshot weatherApiSnapshot() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("key", configuration.weatherApiKey());
        query.put("q", configuration.latitude() + "," + configuration.longitude());
        query.put("days", "7");
        query.put("aqi", "yes");
        query.put("alerts", "no");
        query.put("lang", "fr");
        String json = fetchText(appendQuery(configuration.weatherApiEndpoint(), query), DEFAULT_CACHE_TTL, "WeatherAPI.com Forecast");
        RemoteReading reading = RemoteReading.fromWeatherApi(configuration, json);
        return reading.toSnapshot(DashboardDataStatus.secondaryForecastFallback(providerMode, "WeatherAPI.com", "Open-Meteo indisponible"));
    }

    private DashboardSnapshot visualCrossingSnapshot() {
        URI base = URI.create(configuration.visualCrossingEndpoint().toString().replaceAll("/+$", "")
                + "/" + configuration.latitude() + "," + configuration.longitude());
        Map<String, String> query = new LinkedHashMap<>();
        query.put("unitGroup", "metric");
        query.put("key", configuration.visualCrossingApiKey());
        query.put("contentType", "json");
        query.put("include", "current,days,hours");
        query.put("lang", "fr");
        String json = fetchText(appendQuery(base, query), DEFAULT_CACHE_TTL, "Visual Crossing Timeline");
        RemoteReading reading = RemoteReading.fromVisualCrossing(configuration, json);
        return reading.toSnapshot(DashboardDataStatus.secondaryForecastFallback(providerMode, "Visual Crossing", "Open-Meteo indisponible"));
    }

    private DashboardSnapshot openWeatherSnapshot() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("lat", Double.toString(configuration.latitude()));
        query.put("lon", Double.toString(configuration.longitude()));
        query.put("appid", configuration.openWeatherApiKey());
        query.put("units", "metric");
        query.put("lang", "fr");
        String current = fetchText(appendQuery(configuration.openWeatherCurrentEndpoint(), query), DEFAULT_CACHE_TTL, "OpenWeather Current");
        RemoteReading reading = RemoteReading.fromOpenWeather(configuration, current);
        return reading.toSnapshot(DashboardDataStatus.secondaryForecastFallback(providerMode, "OpenWeather", "Open-Meteo indisponible"));
    }

    private DashboardSnapshot weatherbitSnapshot() {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("lat", Double.toString(configuration.latitude()));
        query.put("lon", Double.toString(configuration.longitude()));
        query.put("key", configuration.weatherbitApiKey());
        query.put("lang", "fr");
        String json = fetchText(appendQuery(configuration.weatherbitEndpoint(), query), DEFAULT_CACHE_TTL, "Weatherbit Current");
        RemoteReading reading = RemoteReading.fromWeatherbit(configuration, json);
        return reading.toSnapshot(DashboardDataStatus.secondaryForecastFallback(providerMode, "Weatherbit", "Open-Meteo indisponible"));
    }

    private DashboardSnapshot pirateWeatherSnapshot() {
        URI base = URI.create(configuration.pirateWeatherEndpoint().toString().replaceAll("/+$", "")
                + "/" + encodePath(configuration.pirateWeatherApiKey())
                + "/" + configuration.latitude() + "," + configuration.longitude());
        Map<String, String> query = new LinkedHashMap<>();
        query.put("units", "si");
        query.put("exclude", "minutely,alerts,flags");
        String json = fetchText(appendQuery(base, query), DEFAULT_CACHE_TTL, "Pirate Weather");
        RemoteReading reading = RemoteReading.fromPirateWeather(configuration, json);
        return reading.toSnapshot(DashboardDataStatus.secondaryForecastFallback(providerMode, "Pirate Weather", "Open-Meteo indisponible"));
    }

    private DashboardSnapshot simulatedFallback(RuntimeException failure) {
        try (SimulatedWeatherDashboardProvider simulated = new SimulatedWeatherDashboardProvider()) {
            return simulated.currentSnapshot().withDataStatus(
                    DashboardDataStatus.remoteFailureFallback(providerMode, "API météo", WeatherProviderFactory.compactFailure(failure))
            );
        }
    }

    private String fetchText(URI uri, Duration ttl, String label) {
        CachedResponse cached = responseCache.get(uri);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            ProviderDiagnostics.info(label + " cache hit for " + uri);
            return cached.body();
        }
        ProviderDiagnostics.info(label + " GET " + uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(configuration.requestTimeout())
                .header("Accept", "application/json")
                .header("User-Agent", configuration.userAgent())
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new RemoteWeatherException(label + " network failure for " + uri, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RemoteWeatherException(label + " request interrupted for " + uri, exception);
        }
        ProviderDiagnostics.info(label + " HTTP " + response.statusCode() + " for " + uri
                + " (" + response.body().length() + " chars)");
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RemoteWeatherException(label + " HTTP " + response.statusCode() + " for " + uri
                    + " · " + excerpt(response.body()));
        }
        responseCache.put(uri, new CachedResponse(response.body(), Instant.now().plus(ttl)));
        return response.body();
    }

    private RuntimeException combinedFailure(List<RuntimeException> failures) {
        RuntimeException combined = new RemoteWeatherException("No configured remote weather provider succeeded.");
        for (RuntimeException failure : failures) {
            combined.addSuppressed(failure);
        }
        return combined;
    }

    private static URI appendQuery(URI base, Map<String, String> values) {
        StringBuilder builder = new StringBuilder(base.toString());
        builder.append(base.getRawQuery() == null ? '?' : '&');
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!first) {
                builder.append('&');
            }
            first = false;
            builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return URI.create(builder.toString());
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodePath(String value) {
        return encode(value).replace("%2F", "/");
    }

    private static String excerpt(String body) {
        if (body == null || body.isBlank()) {
            return "empty body";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return compact.length() > 220 ? compact.substring(0, 217) + "..." : compact;
    }

    private record CachedResponse(String body, Instant expiresAt) {
    }

    private record RemoteSource(String label, boolean enabled, SourceLoader loader) {
        DashboardSnapshot load() {
            return loader.load();
        }
    }

    @FunctionalInterface
    private interface SourceLoader {
        DashboardSnapshot load();
    }

    private static final class RemoteWeatherException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private RemoteWeatherException(String message) {
            super(message);
        }

        private RemoteWeatherException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record RemoteConfiguration(
            double latitude,
            double longitude,
            String city,
            String country,
            ZoneId zoneId,
            URI openMeteoForecastEndpoint,
            URI openMeteoAirQualityEndpoint,
            URI weatherApiEndpoint,
            URI visualCrossingEndpoint,
            URI openWeatherCurrentEndpoint,
            URI weatherbitEndpoint,
            URI pirateWeatherEndpoint,
            String weatherApiKey,
            String visualCrossingApiKey,
            String openWeatherApiKey,
            String weatherbitApiKey,
            String pirateWeatherApiKey,
            String userAgent,
            Duration connectTimeout,
            Duration requestTimeout,
            Duration forecastCacheTtl,
            Duration airQualityCacheTtl,
            boolean weatherApiEnabled,
            boolean visualCrossingEnabled,
            boolean openWeatherEnabled,
            boolean weatherbitEnabled,
            boolean pirateWeatherEnabled
    ) {
        static RemoteConfiguration fromRuntime() {
            double latitude = EnvironmentSettings.decimal("aelia.openmeteo.latitude", "AELIA_OPENMETEO_LATITUDE").orElse(49.4432);
            double longitude = EnvironmentSettings.decimal("aelia.openmeteo.longitude", "AELIA_OPENMETEO_LONGITUDE").orElse(1.0993);
            String city = EnvironmentSettings.text("aelia.openmeteo.city", "AELIA_OPENMETEO_CITY", "Rouen");
            String country = EnvironmentSettings.text("aelia.openmeteo.country", "AELIA_OPENMETEO_COUNTRY", "France");
            ZoneId zoneId = ZoneId.of(EnvironmentSettings.text("aelia.openmeteo.timezone", "AELIA_OPENMETEO_TIMEZONE", "Europe/Paris"));
            String weatherApiKey = EnvironmentSettings.text("aelia.weatherapi.apiKey", "AELIA_WEATHERAPI_API_KEY", "");
            String visualCrossingKey = EnvironmentSettings.text("aelia.visualcrossing.apiKey", "AELIA_VISUALCROSSING_API_KEY", "");
            String openWeatherKey = EnvironmentSettings.text("aelia.openweather.apiKey", "AELIA_OPENWEATHER_API_KEY", "");
            String weatherbitKey = EnvironmentSettings.text("aelia.weatherbit.apiKey", "AELIA_WEATHERBIT_API_KEY", "");
            String pirateWeatherKey = EnvironmentSettings.text("aelia.pirateweather.apiKey", "AELIA_PIRATEWEATHER_API_KEY", "");
            return new RemoteConfiguration(
                    latitude,
                    longitude,
                    city,
                    country,
                    zoneId,
                    uri("aelia.openmeteo.forecastEndpoint", "AELIA_OPENMETEO_FORECAST_ENDPOINT", "https://api.open-meteo.com/v1/forecast"),
                    uri("aelia.openmeteo.airQualityEndpoint", "AELIA_OPENMETEO_AIR_QUALITY_ENDPOINT", "https://air-quality-api.open-meteo.com/v1/air-quality"),
                    uri("aelia.weatherapi.endpoint", "AELIA_WEATHERAPI_ENDPOINT", "https://api.weatherapi.com/v1/forecast.json"),
                    uri("aelia.visualcrossing.endpoint", "AELIA_VISUALCROSSING_ENDPOINT", "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline"),
                    uri("aelia.openweather.currentEndpoint", "AELIA_OPENWEATHER_CURRENT_ENDPOINT", "https://api.openweathermap.org/data/2.5/weather"),
                    uri("aelia.weatherbit.currentEndpoint", "AELIA_WEATHERBIT_CURRENT_ENDPOINT", "https://api.weatherbit.io/v2.0/current"),
                    uri("aelia.pirateweather.endpoint", "AELIA_PIRATEWEATHER_ENDPOINT", "https://api.pirateweather.net/forecast"),
                    weatherApiKey,
                    visualCrossingKey,
                    openWeatherKey,
                    weatherbitKey,
                    pirateWeatherKey,
                    EnvironmentSettings.text("aelia.remote.userAgent", "AELIA_REMOTE_USER_AGENT", "Aelia/0.4.1 github.com/alescis-wuin/Aelia"),
                    EnvironmentSettings.seconds("aelia.openmeteo.connectTimeoutSeconds", "AELIA_OPENMETEO_CONNECT_TIMEOUT_SECONDS", Duration.ofSeconds(20), 1, 120),
                    EnvironmentSettings.seconds("aelia.openmeteo.requestTimeoutSeconds", "AELIA_OPENMETEO_REQUEST_TIMEOUT_SECONDS", Duration.ofSeconds(40), 1, 180),
                    Duration.ofMinutes(15),
                    Duration.ofMinutes(45),
                    EnvironmentSettings.bool("aelia.weatherapi.enabled", "AELIA_WEATHERAPI_ENABLED", true) && !weatherApiKey.isBlank(),
                    EnvironmentSettings.bool("aelia.visualcrossing.enabled", "AELIA_VISUALCROSSING_ENABLED", true) && !visualCrossingKey.isBlank(),
                    EnvironmentSettings.bool("aelia.openweather.enabled", "AELIA_OPENWEATHER_ENABLED", true) && !openWeatherKey.isBlank(),
                    EnvironmentSettings.bool("aelia.weatherbit.enabled", "AELIA_WEATHERBIT_ENABLED", true) && !weatherbitKey.isBlank(),
                    EnvironmentSettings.bool("aelia.pirateweather.enabled", "AELIA_PIRATEWEATHER_ENABLED", true) && !pirateWeatherKey.isBlank()
            );
        }

        private static URI uri(String property, String environment, String fallback) {
            return URI.create(EnvironmentSettings.text(property, environment, fallback));
        }
    }

    private record RemoteReading(
            RemoteConfiguration configuration,
            String source,
            WeatherCondition condition,
            int temperature,
            int apparentTemperature,
            int maxTemperature,
            int minTemperature,
            int humidity,
            int windSpeed,
            String windDirection,
            int windGust,
            int pressure,
            int uvIndex,
            int rainProbability,
            LocalTime sunrise,
            LocalTime sunset,
            List<HourlyForecast> hourlyForecasts,
            List<DailyForecast> dailyForecasts,
            AirQuality airQuality,
            List<PollenRisk> pollenRisks
    ) {
        DashboardSnapshot toSnapshot(DashboardDataStatus status) {
            CurrentWeather currentWeather = new CurrentWeather(
                    configuration.city(),
                    condition.label(),
                    LocalDate.now(configuration.zoneId()),
                    configuration.zoneId(),
                    temperature,
                    maxTemperature,
                    minTemperature,
                    apparentTemperature,
                    sunrise,
                    sunset,
                    humidity,
                    windSpeed,
                    windDirection,
                    windGust,
                    pressure,
                    "Stable",
                    uvIndex,
                    uvIndex >= 6 ? "Protection solaire recommandée." : "Risque UV limité.",
                    airQuality,
                    LocalTime.now(configuration.zoneId())
            );
            List<LocationWeather> locations = locationSummaries(configuration, condition, temperature);
            return new DashboardSnapshot(
                    locations,
                    currentWeather,
                    hourlyForecasts.isEmpty() ? fallbackHourly(configuration, condition, temperature) : hourlyForecasts,
                    dailyForecasts.isEmpty() ? fallbackDaily(configuration, condition, maxTemperature, minTemperature, rainProbability, windSpeed, uvIndex) : dailyForecasts,
                    pollenRisks.isEmpty() ? SimulationCatalog.pollenRisks() : pollenRisks,
                    SimulationCatalog.metrics(),
                    SimulationCatalog.limits(),
                    status.withProviderMode(status.providerMode())
            );
        }

        static RemoteReading fromOpenMeteo(RemoteConfiguration configuration, String forecast, String airQuality) {
            String current = Json.object(forecast, "current").orElseThrow(() -> new RemoteWeatherException("Open-Meteo response has no current object."));
            String daily = Json.object(forecast, "daily").orElse("");
            int weatherCode = requiredNumber(current, "weather_code", "Open-Meteo current.weather_code").intValue();
            WeatherCondition condition = conditionFromOpenMeteo(weatherCode);
            int temperature = rounded(requiredNumber(current, "temperature_2m", "Open-Meteo current.temperature_2m"));
            int apparent = rounded(Json.number(current, "apparent_temperature").orElse((double) temperature));
            int humidity = percentage(Json.number(current, "relative_humidity_2m").orElse(55.0));
            int windSpeed = rounded(Json.number(current, "wind_speed_10m").orElse(0.0));
            String directionLabel = windDirectionLabel(Json.number(current, "wind_direction_10m").orElse(0.0));
            int windGust = rounded(Json.number(current, "wind_gusts_10m").orElse((double) windSpeed));
            int pressure = rounded(Json.number(current, "surface_pressure").orElse(1015.0));
            List<Double> maxValues = Json.numberArray(daily, "temperature_2m_max");
            List<Double> minValues = Json.numberArray(daily, "temperature_2m_min");
            List<Double> uvValues = Json.numberArray(daily, "uv_index_max");
            int max = rounded(first(maxValues, temperature + 2.0));
            int min = rounded(first(minValues, temperature - 3.0));
            int uv = rounded(first(uvValues, 3.0));
            List<Double> rainValues = Json.numberArray(daily, "precipitation_probability_max");
            int rain = percentage(first(rainValues, 0.0));
            LocalTime sunrise = firstTime(Json.stringArray(daily, "sunrise"), LocalTime.of(6, 0));
            LocalTime sunset = firstTime(Json.stringArray(daily, "sunset"), LocalTime.of(21, 0));
            AirQuality aq = airQuality.isBlank() ? defaultAirQuality() : openMeteoAirQuality(airQuality);
            List<PollenRisk> pollen = airQuality.isBlank() ? SimulationCatalog.pollenRisks() : openMeteoPollen(airQuality);
            return new RemoteReading(configuration, "Open-Meteo", condition, temperature, apparent, max, min, humidity,
                    windSpeed, directionLabel, windGust, pressure, uv, rain, sunrise, sunset,
                    openMeteoHourly(forecast), openMeteoDaily(forecast), aq, pollen);
        }

        static RemoteReading fromWeatherApi(RemoteConfiguration configuration, String jsonText) {
            int temp = rounded(Json.number(jsonText, "temp_c").orElse(0.0));
            int apparent = rounded(Json.number(jsonText, "feelslike_c").orElse((double) temp));
            int humidity = percentage(Json.number(jsonText, "humidity").orElse(55.0));
            int wind = rounded(Json.number(jsonText, "wind_kph").orElse(0.0));
            int gust = rounded(Json.number(jsonText, "gust_kph").orElse((double) wind));
            int pressure = rounded(Json.number(jsonText, "pressure_mb").orElse(1015.0));
            WeatherCondition condition = conditionFromText(Json.string(jsonText, "text").orElse(""));
            return compact(configuration, "WeatherAPI.com", condition, temp, apparent, humidity, wind, gust, pressure,
                    rounded(Json.number(jsonText, "uv").orElse(3.0)), defaultAirQuality());
        }

        static RemoteReading fromVisualCrossing(RemoteConfiguration configuration, String jsonText) {
            int temp = rounded(Json.number(jsonText, "temp").orElse(0.0));
            int apparent = rounded(Json.number(jsonText, "feelslike").orElse((double) temp));
            int humidity = percentage(Json.number(jsonText, "humidity").orElse(55.0));
            int wind = rounded(Json.number(jsonText, "windspeed").orElse(0.0));
            int gust = rounded(Json.number(jsonText, "windgust").orElse((double) wind));
            int pressure = rounded(Json.number(jsonText, "pressure").orElse(1015.0));
            WeatherCondition condition = conditionFromText(Json.string(jsonText, "conditions").orElse(""));
            return compact(configuration, "Visual Crossing", condition, temp, apparent, humidity, wind, gust, pressure,
                    rounded(Json.number(jsonText, "uvindex").orElse(3.0)), defaultAirQuality());
        }

        static RemoteReading fromOpenWeather(RemoteConfiguration configuration, String jsonText) {
            int temp = rounded(Json.number(jsonText, "temp").orElse(0.0));
            int apparent = rounded(Json.number(jsonText, "feels_like").orElse((double) temp));
            int humidity = percentage(Json.number(jsonText, "humidity").orElse(55.0));
            int wind = rounded(Json.number(jsonText, "speed").orElse(0.0) * 3.6);
            int pressure = rounded(Json.number(jsonText, "pressure").orElse(1015.0));
            int weatherId = Json.number(jsonText, "id").map(Double::intValue).orElse(800);
            WeatherCondition condition = conditionFromOpenWeather(weatherId);
            return compact(configuration, "OpenWeather", condition, temp, apparent, humidity, wind, wind, pressure, 3, defaultAirQuality());
        }

        static RemoteReading fromWeatherbit(RemoteConfiguration configuration, String jsonText) {
            int temp = rounded(Json.number(jsonText, "temp").orElse(0.0));
            int apparent = rounded(Json.number(jsonText, "app_temp").orElse((double) temp));
            int humidity = percentage(Json.number(jsonText, "rh").orElse(55.0));
            int wind = rounded(Json.number(jsonText, "wind_spd").orElse(0.0) * 3.6);
            int pressure = rounded(Json.number(jsonText, "pres").orElse(1015.0));
            int code = Json.number(jsonText, "code").map(Double::intValue).orElse(800);
            WeatherCondition condition = conditionFromOpenWeather(code);
            return compact(configuration, "Weatherbit", condition, temp, apparent, humidity, wind, wind, pressure, 3, defaultAirQuality());
        }

        static RemoteReading fromPirateWeather(RemoteConfiguration configuration, String jsonText) {
            int temp = rounded(Json.number(jsonText, "temperature").orElse(0.0));
            int apparent = rounded(Json.number(jsonText, "apparentTemperature").orElse((double) temp));
            int humidity = percentage(Json.number(jsonText, "humidity").orElse(0.55) * 100.0);
            int wind = rounded(Json.number(jsonText, "windSpeed").orElse(0.0) * 3.6);
            int gust = rounded(Json.number(jsonText, "windGust").orElse((double) wind));
            int pressure = rounded(Json.number(jsonText, "pressure").orElse(1015.0));
            WeatherCondition condition = conditionFromText(Json.string(jsonText, "icon").orElse(""));
            return compact(configuration, "Pirate Weather", condition, temp, apparent, humidity, wind, gust, pressure,
                    rounded(Json.number(jsonText, "uvIndex").orElse(3.0)), defaultAirQuality());
        }

        private static RemoteReading compact(RemoteConfiguration configuration, String source, WeatherCondition condition,
                                             int temperature, int apparent, int humidity, int wind, int gust, int pressure,
                                             int uv, AirQuality airQuality) {
            return new RemoteReading(configuration, source, condition, temperature, apparent, temperature + 2,
                    temperature - 3, humidity, wind, "N/A", gust, pressure, uv, 0, LocalTime.of(6, 0),
                    LocalTime.of(21, 0), List.of(), List.of(), airQuality, SimulationCatalog.pollenRisks());
        }

        private static AirQuality openMeteoAirQuality(String jsonText) {
            String current = Json.object(jsonText, "current").orElse(jsonText);
            int aqi = rounded(Json.number(current, "european_aqi").orElse(0.0));
            int pm25 = rounded(Json.number(current, "pm2_5").orElse(0.0));
            int pm10 = rounded(Json.number(current, "pm10").orElse(0.0));
            int no2 = rounded(Json.number(current, "nitrogen_dioxide").orElse(0.0));
            return new AirQuality(Math.max(0, aqi), aqi <= 40 ? "BON" : aqi <= 80 ? "MODÉRÉ" : "DÉGRADÉ",
                    Math.max(0, pm25), Math.max(0, pm10), Math.max(0, no2));
        }

        private static AirQuality defaultAirQuality() {
            return new AirQuality(0, "INDISPONIBLE", 0, 0, 0);
        }

        private static List<PollenRisk> openMeteoPollen(String jsonText) {
            String current = Json.object(jsonText, "current").orElse(jsonText);
            return List.of(
                    new PollenRisk("Graminées", pollenLevel(Json.number(current, "grass_pollen").orElse(0.0))),
                    new PollenRisk("Bouleau", pollenLevel(Json.number(current, "birch_pollen").orElse(0.0))),
                    new PollenRisk("Olivier", pollenLevel(Json.number(current, "olive_pollen").orElse(0.0))),
                    new PollenRisk("Ambroisie", pollenLevel(Json.number(current, "ragweed_pollen").orElse(0.0)))
            );
        }
    }

    private static List<LocationWeather> locationSummaries(RemoteConfiguration configuration, WeatherCondition condition, int temperature) {
        List<LocationWeather> locations = new ArrayList<>();
        locations.add(new LocationWeather(configuration.city(), configuration.country(), condition, temperature, true,
                configuration.latitude(), configuration.longitude()));
        for (LocationWeather location : SimulationCatalog.locations()) {
            if (!location.city().equalsIgnoreCase(configuration.city())) {
                locations.add(new LocationWeather(location.city(), location.country(), location.condition(),
                        location.temperatureCelsius(), false, location.latitude(), location.longitude()));
            }
        }
        return locations;
    }

    private static List<HourlyForecast> openMeteoHourly(String forecast) {
        String hourly = Json.object(forecast, "hourly").orElse("");
        List<String> times = Json.stringArray(hourly, "time");
        List<Double> temperatures = Json.numberArray(hourly, "temperature_2m");
        List<Double> codes = Json.numberArray(hourly, "weather_code");
        List<HourlyForecast> values = new ArrayList<>();
        int count = Math.min(8, Math.min(times.size(), temperatures.size()));
        for (int index = 0; index < count; index++) {
            values.add(new HourlyForecast(timeOf(times.get(index)), conditionFromOpenMeteo(intAt(codes, index, 2)),
                    rounded(temperatures.get(index)), index == 0));
        }
        return values;
    }

    private static List<DailyForecast> openMeteoDaily(String forecast) {
        String daily = Json.object(forecast, "daily").orElse("");
        List<String> days = Json.stringArray(daily, "time");
        List<Double> codes = Json.numberArray(daily, "weather_code");
        List<Double> max = Json.numberArray(daily, "temperature_2m_max");
        List<Double> min = Json.numberArray(daily, "temperature_2m_min");
        List<Double> rain = Json.numberArray(daily, "precipitation_probability_max");
        List<Double> wind = Json.numberArray(daily, "wind_speed_10m_max");
        List<Double> uv = Json.numberArray(daily, "uv_index_max");
        List<DailyForecast> values = new ArrayList<>();
        int count = Math.min(7, Math.min(days.size(), Math.min(max.size(), min.size())));
        for (int index = 0; index < count; index++) {
            LocalDate date = LocalDate.parse(days.get(index).substring(0, 10));
            values.add(new DailyForecast(date, dayLabel(date), conditionFromOpenMeteo(intAt(codes, index, 2)),
                    rounded(max.get(index)), rounded(min.get(index)), percentage(firstAt(rain, index, 0.0)),
                    rounded(firstAt(wind, index, 0.0)), rounded(firstAt(uv, index, 3.0)), index == 0));
        }
        return values;
    }

    private static List<HourlyForecast> fallbackHourly(RemoteConfiguration configuration, WeatherCondition condition, int temperature) {
        List<HourlyForecast> values = new ArrayList<>();
        LocalTime start = LocalTime.now(configuration.zoneId()).withMinute(0).withSecond(0).withNano(0);
        for (int index = 0; index < 8; index++) {
            values.add(new HourlyForecast(start.plusHours(index * 3L), condition, temperature, index == 0));
        }
        return values;
    }

    private static List<DailyForecast> fallbackDaily(RemoteConfiguration configuration, WeatherCondition condition, int max, int min,
                                                     int rain, int wind, int uv) {
        List<DailyForecast> values = new ArrayList<>();
        LocalDate start = LocalDate.now(configuration.zoneId());
        for (int index = 0; index < 7; index++) {
            LocalDate date = start.plusDays(index);
            values.add(new DailyForecast(date, dayLabel(date), condition, max, min, rain, wind, uv, index == 0));
        }
        return values;
    }

    private static WeatherCondition conditionFromOpenMeteo(int code) {
        if (code == 0) {
            return WeatherCondition.SUNNY;
        }
        if (code == 1 || code == 2) {
            return WeatherCondition.PARTLY_CLOUDY;
        }
        if (code == 3 || code == 45 || code == 48) {
            return WeatherCondition.CLOUDY;
        }
        if (code >= 95) {
            return WeatherCondition.STORMY;
        }
        if (code >= 71 && code <= 86) {
            return WeatherCondition.SNOWY;
        }
        if (code >= 51 && code <= 67 || code >= 80 && code <= 82) {
            return WeatherCondition.RAINY;
        }
        return WeatherCondition.CLOUDY;
    }

    private static WeatherCondition conditionFromOpenWeather(int code) {
        if (code >= 200 && code < 300) {
            return WeatherCondition.STORMY;
        }
        if (code >= 300 && code < 600) {
            return WeatherCondition.RAINY;
        }
        if (code >= 600 && code < 700) {
            return WeatherCondition.SNOWY;
        }
        if (code == 800) {
            return WeatherCondition.SUNNY;
        }
        if (code > 800) {
            return WeatherCondition.CLOUDY;
        }
        return WeatherCondition.PARTLY_CLOUDY;
    }

    private static WeatherCondition conditionFromText(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (normalized.contains("thunder") || normalized.contains("orage")) {
            return WeatherCondition.STORMY;
        }
        if (normalized.contains("snow") || normalized.contains("neige")) {
            return WeatherCondition.SNOWY;
        }
        if (normalized.contains("rain") || normalized.contains("pluie") || normalized.contains("drizzle")) {
            return WeatherCondition.RAINY;
        }
        if (normalized.contains("cloud") || normalized.contains("nuage") || normalized.contains("overcast")) {
            return WeatherCondition.CLOUDY;
        }
        if (normalized.contains("clear") || normalized.contains("sun") || normalized.contains("soleil")) {
            return WeatherCondition.SUNNY;
        }
        return WeatherCondition.PARTLY_CLOUDY;
    }

    private static PollenLevel pollenLevel(double value) {
        if (value <= 0.0) {
            return PollenLevel.NONE;
        }
        if (value < 20.0) {
            return PollenLevel.LOW;
        }
        if (value < 80.0) {
            return PollenLevel.MODERATE;
        }
        if (value < 150.0) {
            return PollenLevel.HIGH;
        }
        return PollenLevel.EXTREME;
    }

    private static String windDirectionLabel(double degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
        int index = (int) Math.round((((degrees % 360.0) + 360.0) % 360.0) / 45.0) % directions.length;
        return directions[index];
    }

    private static String dayLabel(LocalDate date) {
        String day = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRANCE);
        return capitalize(day) + " " + date.getDayOfMonth();
    }

    private static String capitalize(String value) {
        return value == null || value.isBlank() ? "" : value.substring(0, 1).toUpperCase(Locale.FRANCE) + value.substring(1);
    }

    private static LocalTime firstTime(List<String> values, LocalTime fallback) {
        return values.isEmpty() ? fallback : timeOf(values.get(0));
    }

    private static LocalTime timeOf(String value) {
        if (value == null || value.isBlank()) {
            return LocalTime.NOON;
        }
        String normalized = value.trim();
        if (normalized.length() >= 16 && normalized.charAt(10) == 'T') {
            return LocalDateTime.parse(normalized.substring(0, 16)).toLocalTime();
        }
        if (normalized.length() >= 5 && normalized.charAt(2) == ':') {
            return LocalTime.parse(normalized.substring(0, 5));
        }
        return LocalTime.NOON;
    }

    private static int rounded(double value) {
        return (int) Math.round(value);
    }

    private static int percentage(double value) {
        return Math.max(0, Math.min(100, rounded(value)));
    }

    private static double first(List<Double> values, double fallback) {
        return values.isEmpty() ? fallback : values.get(0);
    }

    private static double firstAt(List<Double> values, int index, double fallback) {
        return index >= 0 && index < values.size() ? values.get(index) : fallback;
    }

    private static int intAt(List<Double> values, int index, int fallback) {
        return index >= 0 && index < values.size() ? values.get(index).intValue() : fallback;
    }

    private static Double requiredNumber(String text, String key, String label) {
        return Json.number(text, key).orElseThrow(() -> new RemoteWeatherException(label + " missing in remote response."));
    }

    private static final class Json {
        private Json() {
        }

        static Optional<String> object(String text, String key) {
            int index = keyIndex(text, key);
            if (index < 0) {
                return Optional.empty();
            }
            int colon = text.indexOf(':', index);
            if (colon < 0) {
                return Optional.empty();
            }
            int open = text.indexOf('{', colon + 1);
            if (open < 0) {
                return Optional.empty();
            }
            int depth = 0;
            boolean inString = false;
            boolean escape = false;
            for (int cursor = open; cursor < text.length(); cursor++) {
                char c = text.charAt(cursor);
                if (escape) {
                    escape = false;
                    continue;
                }
                if (c == '\\') {
                    escape = true;
                    continue;
                }
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                if (inString) {
                    continue;
                }
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return Optional.of(text.substring(open + 1, cursor));
                    }
                }
            }
            return Optional.empty();
        }

        static Optional<Double> number(String text, String key) {
            int index = keyIndex(text, key);
            if (index < 0) {
                return Optional.empty();
            }
            int colon = text.indexOf(':', index);
            if (colon < 0) {
                return Optional.empty();
            }
            int cursor = colon + 1;
            while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
                cursor++;
            }
            int start = cursor;
            while (cursor < text.length()) {
                char c = text.charAt(cursor);
                if (!(Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'E' || c == 'e')) {
                    break;
                }
                cursor++;
            }
            if (start == cursor) {
                return Optional.empty();
            }
            try {
                return Optional.of(Double.parseDouble(text.substring(start, cursor)));
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
        }

        static Optional<String> string(String text, String key) {
            int index = keyIndex(text, key);
            if (index < 0) {
                return Optional.empty();
            }
            int colon = text.indexOf(':', index);
            if (colon < 0) {
                return Optional.empty();
            }
            int quote = text.indexOf('"', colon + 1);
            if (quote < 0) {
                return Optional.empty();
            }
            StringBuilder value = new StringBuilder();
            boolean escape = false;
            for (int cursor = quote + 1; cursor < text.length(); cursor++) {
                char c = text.charAt(cursor);
                if (escape) {
                    value.append(c);
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    return Optional.of(value.toString());
                } else {
                    value.append(c);
                }
            }
            return Optional.empty();
        }

        static List<Double> numberArray(String text, String key) {
            return arrayBody(text, key).map(body -> {
                List<Double> values = new ArrayList<>();
                for (String raw : body.split(",")) {
                    String value = raw.trim();
                    if (value.isEmpty() || value.equals("null")) {
                        continue;
                    }
                    try {
                        values.add(Double.parseDouble(value));
                    } catch (NumberFormatException ignored) {
                        // Ignore non-numeric values in lenient provider parsing.
                    }
                }
                return values;
            }).orElse(List.of());
        }

        static List<String> stringArray(String text, String key) {
            return arrayBody(text, key).map(body -> {
                List<String> values = new ArrayList<>();
                int cursor = 0;
                while (cursor < body.length()) {
                    int quote = body.indexOf('"', cursor);
                    if (quote < 0) {
                        break;
                    }
                    StringBuilder value = new StringBuilder();
                    boolean escape = false;
                    for (int i = quote + 1; i < body.length(); i++) {
                        char c = body.charAt(i);
                        if (escape) {
                            value.append(c);
                            escape = false;
                        } else if (c == '\\') {
                            escape = true;
                        } else if (c == '"') {
                            values.add(value.toString());
                            cursor = i + 1;
                            break;
                        } else {
                            value.append(c);
                        }
                    }
                    cursor++;
                }
                return values;
            }).orElse(List.of());
        }

        private static Optional<String> arrayBody(String text, String key) {
            int index = keyIndex(text, key);
            if (index < 0) {
                return Optional.empty();
            }
            int open = text.indexOf('[', index);
            if (open < 0) {
                return Optional.empty();
            }
            int depth = 0;
            boolean inString = false;
            boolean escape = false;
            for (int cursor = open; cursor < text.length(); cursor++) {
                char c = text.charAt(cursor);
                if (escape) {
                    escape = false;
                    continue;
                }
                if (c == '\\') {
                    escape = true;
                    continue;
                }
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                if (inString) {
                    continue;
                }
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        return Optional.of(text.substring(open + 1, cursor));
                    }
                }
            }
            return Optional.empty();
        }

        private static int keyIndex(String text, String key) {
            if (text == null || key == null) {
                return -1;
            }
            return text.indexOf('"' + key + '"');
        }
    }
}
