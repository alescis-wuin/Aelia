package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.WeatherCondition;
import fr.alescis.aelia.provider.EnvironmentSettings;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime configuration for remote weather providers used by the Open-Meteo based chain.
 */
public final class OpenMeteoConfiguration {
    private static final URI DEFAULT_FORECAST_ENDPOINT = URI.create("https://api.open-meteo.com/v1/forecast");
    private static final URI DEFAULT_AIR_QUALITY_ENDPOINT = URI.create("https://air-quality-api.open-meteo.com/v1/air-quality");
    private static final URI DEFAULT_ARCHIVE_ENDPOINT = URI.create("https://archive-api.open-meteo.com/v1/archive");
    private static final URI DEFAULT_GEOCODING_ENDPOINT = URI.create("https://geocoding-api.open-meteo.com/v1/search");
    private static final URI DEFAULT_MET_NORWAY_ENDPOINT = URI.create("https://api.met.no/weatherapi/locationforecast/2.0/compact");
    private static final URI DEFAULT_WEATHER_API_ENDPOINT = URI.create("https://api.weatherapi.com/v1/forecast.json");
    private static final URI DEFAULT_OPENWEATHER_CURRENT_ENDPOINT = URI.create("https://api.openweathermap.org/data/2.5/weather");
    private static final URI DEFAULT_OPENWEATHER_FORECAST_ENDPOINT = URI.create("https://api.openweathermap.org/data/2.5/forecast");
    private static final URI DEFAULT_VISUAL_CROSSING_TIMELINE_ENDPOINT = URI.create("https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline");
    private static final URI DEFAULT_WEATHERBIT_CURRENT_ENDPOINT = URI.create("https://api.weatherbit.io/v2.0/current");
    private static final URI DEFAULT_WEATHERBIT_DAILY_ENDPOINT = URI.create("https://api.weatherbit.io/v2.0/forecast/daily");
    private static final URI DEFAULT_PIRATE_WEATHER_ENDPOINT = URI.create("https://api.pirateweather.net/forecast");
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String DEFAULT_USER_AGENT = "Aelia/0.3.20 github.com/alescis-wuin/Aelia";

    private final URI forecastEndpoint;
    private final URI airQualityEndpoint;
    private final URI archiveEndpoint;
    private final URI geocodingEndpoint;
    private final URI metNorwayEndpoint;
    private final URI weatherApiEndpoint;
    private final URI openWeatherCurrentEndpoint;
    private final URI openWeatherForecastEndpoint;
    private final URI visualCrossingTimelineEndpoint;
    private final URI weatherbitCurrentEndpoint;
    private final URI weatherbitDailyEndpoint;
    private final URI pirateWeatherEndpoint;
    private final String apiKey;
    private final String weatherApiKey;
    private final String openWeatherApiKey;
    private final String visualCrossingApiKey;
    private final String weatherbitApiKey;
    private final String pirateWeatherApiKey;
    private final String userAgent;
    private final List<OpenMeteoLocation> locations;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final Duration forecastCacheTtl;
    private final Duration airQualityCacheTtl;
    private final Duration archiveCacheTtl;
    private final Duration geocodingCacheTtl;
    private final Duration metNorwayCacheTtl;
    private final Duration weatherApiCacheTtl;
    private final Duration openWeatherCacheTtl;
    private final Duration visualCrossingCacheTtl;
    private final Duration weatherbitCacheTtl;
    private final Duration pirateWeatherCacheTtl;
    private final int archiveFallbackLagDays;
    private final boolean metNorwayEnabled;
    private final boolean weatherApiEnabled;
    private final boolean openWeatherEnabled;
    private final boolean visualCrossingEnabled;
    private final boolean weatherbitEnabled;
    private final boolean pirateWeatherEnabled;

    /**
     * Backward-compatible constructor kept for existing tests and local integrations.
     */
    public OpenMeteoConfiguration(
            URI forecastEndpoint,
            URI airQualityEndpoint,
            URI archiveEndpoint,
            URI geocodingEndpoint,
            String apiKey,
            String userAgent,
            List<OpenMeteoLocation> locations,
            Duration connectTimeout,
            Duration requestTimeout,
            Duration forecastCacheTtl,
            Duration airQualityCacheTtl,
            Duration archiveCacheTtl,
            Duration geocodingCacheTtl,
            int archiveFallbackLagDays
    ) {
        this(
                forecastEndpoint,
                airQualityEndpoint,
                archiveEndpoint,
                geocodingEndpoint,
                DEFAULT_MET_NORWAY_ENDPOINT,
                DEFAULT_WEATHER_API_ENDPOINT,
                DEFAULT_OPENWEATHER_CURRENT_ENDPOINT,
                DEFAULT_OPENWEATHER_FORECAST_ENDPOINT,
                DEFAULT_VISUAL_CROSSING_TIMELINE_ENDPOINT,
                DEFAULT_WEATHERBIT_CURRENT_ENDPOINT,
                DEFAULT_WEATHERBIT_DAILY_ENDPOINT,
                DEFAULT_PIRATE_WEATHER_ENDPOINT,
                apiKey,
                "",
                "",
                "",
                "",
                "",
                userAgent,
                locations,
                connectTimeout,
                requestTimeout,
                forecastCacheTtl,
                airQualityCacheTtl,
                archiveCacheTtl,
                geocodingCacheTtl,
                Duration.ofMinutes(30),
                Duration.ofMinutes(20),
                Duration.ofMinutes(20),
                Duration.ofMinutes(20),
                Duration.ofMinutes(30),
                Duration.ofMinutes(20),
                archiveFallbackLagDays,
                true,
                false,
                false,
                false,
                false,
                false
        );
    }

    public OpenMeteoConfiguration(
            URI forecastEndpoint,
            URI airQualityEndpoint,
            URI archiveEndpoint,
            URI geocodingEndpoint,
            URI metNorwayEndpoint,
            URI weatherApiEndpoint,
            URI openWeatherCurrentEndpoint,
            URI openWeatherForecastEndpoint,
            URI visualCrossingTimelineEndpoint,
            URI weatherbitCurrentEndpoint,
            URI weatherbitDailyEndpoint,
            URI pirateWeatherEndpoint,
            String apiKey,
            String weatherApiKey,
            String openWeatherApiKey,
            String visualCrossingApiKey,
            String weatherbitApiKey,
            String pirateWeatherApiKey,
            String userAgent,
            List<OpenMeteoLocation> locations,
            Duration connectTimeout,
            Duration requestTimeout,
            Duration forecastCacheTtl,
            Duration airQualityCacheTtl,
            Duration archiveCacheTtl,
            Duration geocodingCacheTtl,
            Duration metNorwayCacheTtl,
            Duration weatherApiCacheTtl,
            Duration openWeatherCacheTtl,
            Duration visualCrossingCacheTtl,
            Duration weatherbitCacheTtl,
            Duration pirateWeatherCacheTtl,
            int archiveFallbackLagDays,
            boolean metNorwayEnabled,
            boolean weatherApiEnabled,
            boolean openWeatherEnabled,
            boolean visualCrossingEnabled,
            boolean weatherbitEnabled,
            boolean pirateWeatherEnabled
    ) {
        this.forecastEndpoint = Objects.requireNonNull(forecastEndpoint, "forecastEndpoint");
        this.airQualityEndpoint = Objects.requireNonNull(airQualityEndpoint, "airQualityEndpoint");
        this.archiveEndpoint = Objects.requireNonNull(archiveEndpoint, "archiveEndpoint");
        this.geocodingEndpoint = Objects.requireNonNull(geocodingEndpoint, "geocodingEndpoint");
        this.metNorwayEndpoint = Objects.requireNonNull(metNorwayEndpoint, "metNorwayEndpoint");
        this.weatherApiEndpoint = Objects.requireNonNull(weatherApiEndpoint, "weatherApiEndpoint");
        this.openWeatherCurrentEndpoint = Objects.requireNonNull(openWeatherCurrentEndpoint, "openWeatherCurrentEndpoint");
        this.openWeatherForecastEndpoint = Objects.requireNonNull(openWeatherForecastEndpoint, "openWeatherForecastEndpoint");
        this.visualCrossingTimelineEndpoint = Objects.requireNonNull(visualCrossingTimelineEndpoint, "visualCrossingTimelineEndpoint");
        this.weatherbitCurrentEndpoint = Objects.requireNonNull(weatherbitCurrentEndpoint, "weatherbitCurrentEndpoint");
        this.weatherbitDailyEndpoint = Objects.requireNonNull(weatherbitDailyEndpoint, "weatherbitDailyEndpoint");
        this.pirateWeatherEndpoint = Objects.requireNonNull(pirateWeatherEndpoint, "pirateWeatherEndpoint");
        this.apiKey = trimmed(apiKey);
        this.weatherApiKey = trimmed(weatherApiKey);
        this.openWeatherApiKey = trimmed(openWeatherApiKey);
        this.visualCrossingApiKey = trimmed(visualCrossingApiKey);
        this.weatherbitApiKey = trimmed(weatherbitApiKey);
        this.pirateWeatherApiKey = trimmed(pirateWeatherApiKey);
        this.userAgent = userAgent == null || userAgent.isBlank() ? DEFAULT_USER_AGENT : userAgent.trim();
        this.locations = normalizeLocations(locations);
        this.connectTimeout = positive(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
        this.requestTimeout = positive(requestTimeout, DEFAULT_REQUEST_TIMEOUT);
        this.forecastCacheTtl = positive(forecastCacheTtl, Duration.ofMinutes(15));
        this.airQualityCacheTtl = positive(airQualityCacheTtl, Duration.ofMinutes(45));
        this.archiveCacheTtl = positive(archiveCacheTtl, Duration.ofHours(6));
        this.geocodingCacheTtl = positive(geocodingCacheTtl, Duration.ofDays(1));
        this.metNorwayCacheTtl = positive(metNorwayCacheTtl, Duration.ofMinutes(30));
        this.weatherApiCacheTtl = positive(weatherApiCacheTtl, Duration.ofMinutes(20));
        this.openWeatherCacheTtl = positive(openWeatherCacheTtl, Duration.ofMinutes(20));
        this.visualCrossingCacheTtl = positive(visualCrossingCacheTtl, Duration.ofMinutes(20));
        this.weatherbitCacheTtl = positive(weatherbitCacheTtl, Duration.ofMinutes(30));
        this.pirateWeatherCacheTtl = positive(pirateWeatherCacheTtl, Duration.ofMinutes(20));
        this.archiveFallbackLagDays = Math.max(1, archiveFallbackLagDays);
        this.metNorwayEnabled = metNorwayEnabled;
        this.weatherApiEnabled = weatherApiEnabled && !this.weatherApiKey.isBlank();
        this.openWeatherEnabled = openWeatherEnabled && !this.openWeatherApiKey.isBlank();
        this.visualCrossingEnabled = visualCrossingEnabled && !this.visualCrossingApiKey.isBlank();
        this.weatherbitEnabled = weatherbitEnabled && !this.weatherbitApiKey.isBlank();
        this.pirateWeatherEnabled = pirateWeatherEnabled && !this.pirateWeatherApiKey.isBlank();
    }

    public static OpenMeteoConfiguration fromSystemProperties() {
        List<OpenMeteoLocation> locations = defaultLocations();
        Optional<Double> latitude = numberSetting("aelia.openmeteo.latitude", "AELIA_OPENMETEO_LATITUDE");
        Optional<Double> longitude = numberSetting("aelia.openmeteo.longitude", "AELIA_OPENMETEO_LONGITUDE");
        if (latitude.isPresent() && longitude.isPresent()) {
            String city = textSetting("aelia.openmeteo.city", "AELIA_OPENMETEO_CITY").orElse("Localisation");
            String country = textSetting("aelia.openmeteo.country", "AELIA_OPENMETEO_COUNTRY").orElse("Personnalisée");
            ZoneId zone = textSetting("aelia.openmeteo.timezone", "AELIA_OPENMETEO_TIMEZONE")
                    .map(ZoneId::of)
                    .orElse(ZoneId.systemDefault());
            List<OpenMeteoLocation> customized = new ArrayList<>(locations.size());
            customized.add(new OpenMeteoLocation(city, country, latitude.get(), longitude.get(), zone,
                    WeatherCondition.CLOUDY, 0, true));
            for (int index = 1; index < locations.size(); index++) {
                customized.add(locations.get(index).withSelected(false));
            }
            locations = customized;
        }

        URI forecastEndpoint = uriSetting("aelia.openmeteo.forecastEndpoint", "AELIA_OPENMETEO_FORECAST_ENDPOINT")
                .orElse(DEFAULT_FORECAST_ENDPOINT);
        URI airQualityEndpoint = uriSetting("aelia.openmeteo.airQualityEndpoint", "AELIA_OPENMETEO_AIR_QUALITY_ENDPOINT")
                .orElse(DEFAULT_AIR_QUALITY_ENDPOINT);
        URI archiveEndpoint = uriSetting("aelia.openmeteo.archiveEndpoint", "AELIA_OPENMETEO_ARCHIVE_ENDPOINT")
                .orElse(DEFAULT_ARCHIVE_ENDPOINT);
        URI geocodingEndpoint = uriSetting("aelia.openmeteo.geocodingEndpoint", "AELIA_OPENMETEO_GEOCODING_ENDPOINT")
                .orElse(DEFAULT_GEOCODING_ENDPOINT);
        URI metNorwayEndpoint = uriSetting("aelia.metnorway.endpoint", "AELIA_METNORWAY_ENDPOINT")
                .orElse(DEFAULT_MET_NORWAY_ENDPOINT);
        URI weatherApiEndpoint = uriSetting("aelia.weatherapi.endpoint", "AELIA_WEATHERAPI_ENDPOINT")
                .orElse(DEFAULT_WEATHER_API_ENDPOINT);
        URI openWeatherCurrentEndpoint = uriSetting("aelia.openweather.currentEndpoint", "AELIA_OPENWEATHER_CURRENT_ENDPOINT")
                .orElse(DEFAULT_OPENWEATHER_CURRENT_ENDPOINT);
        URI openWeatherForecastEndpoint = uriSetting("aelia.openweather.forecastEndpoint", "AELIA_OPENWEATHER_FORECAST_ENDPOINT")
                .orElse(DEFAULT_OPENWEATHER_FORECAST_ENDPOINT);
        URI visualCrossingTimelineEndpoint = uriSetting("aelia.visualcrossing.timelineEndpoint", "AELIA_VISUALCROSSING_TIMELINE_ENDPOINT")
                .orElse(DEFAULT_VISUAL_CROSSING_TIMELINE_ENDPOINT);
        URI weatherbitCurrentEndpoint = uriSetting("aelia.weatherbit.currentEndpoint", "AELIA_WEATHERBIT_CURRENT_ENDPOINT")
                .orElse(DEFAULT_WEATHERBIT_CURRENT_ENDPOINT);
        URI weatherbitDailyEndpoint = uriSetting("aelia.weatherbit.dailyEndpoint", "AELIA_WEATHERBIT_DAILY_ENDPOINT")
                .orElse(DEFAULT_WEATHERBIT_DAILY_ENDPOINT);
        URI pirateWeatherEndpoint = uriSetting("aelia.pirateweather.endpoint", "AELIA_PIRATEWEATHER_ENDPOINT")
                .orElse(DEFAULT_PIRATE_WEATHER_ENDPOINT);
        String apiKey = textSetting("aelia.openmeteo.apiKey", "AELIA_OPENMETEO_API_KEY").orElse("");
        String weatherApiKey = textSetting("aelia.weatherapi.apiKey", "AELIA_WEATHERAPI_API_KEY").orElse("");
        String openWeatherApiKey = textSetting("aelia.openweather.apiKey", "AELIA_OPENWEATHER_API_KEY").orElse("");
        String visualCrossingApiKey = textSetting("aelia.visualcrossing.apiKey", "AELIA_VISUALCROSSING_API_KEY").orElse("");
        String weatherbitApiKey = textSetting("aelia.weatherbit.apiKey", "AELIA_WEATHERBIT_API_KEY").orElse("");
        String pirateWeatherApiKey = textSetting("aelia.pirateweather.apiKey", "AELIA_PIRATEWEATHER_API_KEY").orElse("");
        String userAgent = textSetting("aelia.openmeteo.userAgent", "AELIA_OPENMETEO_USER_AGENT")
                .orElse(DEFAULT_USER_AGENT);
        Duration connectTimeout = secondsSetting(
                "aelia.openmeteo.connectTimeoutSeconds",
                "AELIA_OPENMETEO_CONNECT_TIMEOUT_SECONDS",
                DEFAULT_CONNECT_TIMEOUT
        );
        Duration requestTimeout = secondsSetting(
                "aelia.openmeteo.requestTimeoutSeconds",
                "AELIA_OPENMETEO_REQUEST_TIMEOUT_SECONDS",
                DEFAULT_REQUEST_TIMEOUT
        );
        int archiveFallbackLagDays = integerSetting(
                "aelia.openmeteo.archiveFallbackLagDays",
                "AELIA_OPENMETEO_ARCHIVE_FALLBACK_LAG_DAYS",
                2
        );
        boolean metNorwayEnabled = booleanSetting("aelia.metnorway.enabled", "AELIA_METNORWAY_ENABLED", false);
        boolean weatherApiEnabled = booleanSetting("aelia.weatherapi.enabled", "AELIA_WEATHERAPI_ENABLED", true);
        boolean openWeatherEnabled = booleanSetting("aelia.openweather.enabled", "AELIA_OPENWEATHER_ENABLED", true);
        boolean visualCrossingEnabled = booleanSetting("aelia.visualcrossing.enabled", "AELIA_VISUALCROSSING_ENABLED", true);
        boolean weatherbitEnabled = booleanSetting("aelia.weatherbit.enabled", "AELIA_WEATHERBIT_ENABLED", true);
        boolean pirateWeatherEnabled = booleanSetting("aelia.pirateweather.enabled", "AELIA_PIRATEWEATHER_ENABLED", true);

        return new OpenMeteoConfiguration(
                forecastEndpoint,
                airQualityEndpoint,
                archiveEndpoint,
                geocodingEndpoint,
                metNorwayEndpoint,
                weatherApiEndpoint,
                openWeatherCurrentEndpoint,
                openWeatherForecastEndpoint,
                visualCrossingTimelineEndpoint,
                weatherbitCurrentEndpoint,
                weatherbitDailyEndpoint,
                pirateWeatherEndpoint,
                apiKey,
                weatherApiKey,
                openWeatherApiKey,
                visualCrossingApiKey,
                weatherbitApiKey,
                pirateWeatherApiKey,
                userAgent,
                locations,
                connectTimeout,
                requestTimeout,
                Duration.ofMinutes(15),
                Duration.ofMinutes(45),
                Duration.ofHours(6),
                Duration.ofDays(1),
                Duration.ofMinutes(30),
                Duration.ofMinutes(20),
                Duration.ofMinutes(20),
                Duration.ofMinutes(20),
                Duration.ofMinutes(30),
                Duration.ofMinutes(20),
                archiveFallbackLagDays,
                metNorwayEnabled,
                weatherApiEnabled,
                openWeatherEnabled,
                visualCrossingEnabled,
                weatherbitEnabled,
                pirateWeatherEnabled
        );
    }

    public URI forecastEndpoint() {
        return forecastEndpoint;
    }

    public URI airQualityEndpoint() {
        return airQualityEndpoint;
    }

    public URI archiveEndpoint() {
        return archiveEndpoint;
    }

    public URI geocodingEndpoint() {
        return geocodingEndpoint;
    }

    public URI metNorwayEndpoint() {
        return metNorwayEndpoint;
    }

    public URI weatherApiEndpoint() {
        return weatherApiEndpoint;
    }

    public URI openWeatherCurrentEndpoint() {
        return openWeatherCurrentEndpoint;
    }

    public URI openWeatherForecastEndpoint() {
        return openWeatherForecastEndpoint;
    }

    public URI visualCrossingTimelineEndpoint() {
        return visualCrossingTimelineEndpoint;
    }

    public URI weatherbitCurrentEndpoint() {
        return weatherbitCurrentEndpoint;
    }

    public URI weatherbitDailyEndpoint() {
        return weatherbitDailyEndpoint;
    }

    public URI pirateWeatherEndpoint() {
        return pirateWeatherEndpoint;
    }

    public Optional<String> apiKey() {
        return optional(apiKey);
    }

    public Optional<String> weatherApiKey() {
        return optional(weatherApiKey);
    }

    public Optional<String> openWeatherApiKey() {
        return optional(openWeatherApiKey);
    }

    public Optional<String> visualCrossingApiKey() {
        return optional(visualCrossingApiKey);
    }

    public Optional<String> weatherbitApiKey() {
        return optional(weatherbitApiKey);
    }

    public Optional<String> pirateWeatherApiKey() {
        return optional(pirateWeatherApiKey);
    }

    public String userAgent() {
        return userAgent;
    }

    public List<OpenMeteoLocation> locations() {
        return locations;
    }

    public OpenMeteoLocation selectedLocation() {
        return locations.stream().filter(OpenMeteoLocation::selected).findFirst().orElse(locations.get(0));
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration requestTimeout() {
        return requestTimeout;
    }

    public Duration forecastCacheTtl() {
        return forecastCacheTtl;
    }

    public Duration airQualityCacheTtl() {
        return airQualityCacheTtl;
    }

    public Duration archiveCacheTtl() {
        return archiveCacheTtl;
    }

    public Duration geocodingCacheTtl() {
        return geocodingCacheTtl;
    }

    public Duration metNorwayCacheTtl() {
        return metNorwayCacheTtl;
    }

    public Duration weatherApiCacheTtl() {
        return weatherApiCacheTtl;
    }

    public Duration openWeatherCacheTtl() {
        return openWeatherCacheTtl;
    }

    public Duration visualCrossingCacheTtl() {
        return visualCrossingCacheTtl;
    }

    public Duration weatherbitCacheTtl() {
        return weatherbitCacheTtl;
    }

    public Duration pirateWeatherCacheTtl() {
        return pirateWeatherCacheTtl;
    }

    public int archiveFallbackLagDays() {
        return archiveFallbackLagDays;
    }

    public boolean metNorwayEnabled() {
        return metNorwayEnabled;
    }

    public boolean weatherApiEnabled() {
        return weatherApiEnabled;
    }

    public boolean openWeatherEnabled() {
        return openWeatherEnabled;
    }

    public boolean visualCrossingEnabled() {
        return visualCrossingEnabled;
    }

    public boolean weatherbitEnabled() {
        return weatherbitEnabled;
    }

    public boolean pirateWeatherEnabled() {
        return pirateWeatherEnabled;
    }

    private static List<OpenMeteoLocation> defaultLocations() {
        return List.of(
                new OpenMeteoLocation("Paris", "France", 48.8566, 2.3522, ZoneId.of("Europe/Paris"), WeatherCondition.SUNNY, 24, true),
                new OpenMeteoLocation("Tokyo", "Japon", 35.6764, 139.6500, ZoneId.of("Asia/Tokyo"), WeatherCondition.PARTLY_CLOUDY, 19, false),
                new OpenMeteoLocation("New York", "USA", 40.7128, -74.0060, ZoneId.of("America/New_York"), WeatherCondition.RAINY, 11, false),
                new OpenMeteoLocation("Dubaï", "EAU", 25.2048, 55.2708, ZoneId.of("Asia/Dubai"), WeatherCondition.SUNNY, 38, false)
        );
    }

    private static List<OpenMeteoLocation> normalizeLocations(List<OpenMeteoLocation> source) {
        List<OpenMeteoLocation> normalized = List.copyOf(Objects.requireNonNull(source, "locations"));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("At least one location is required.");
        }
        long selectedCount = normalized.stream().filter(OpenMeteoLocation::selected).count();
        if (selectedCount == 1L) {
            return normalized;
        }
        List<OpenMeteoLocation> fixed = new ArrayList<>(normalized.size());
        for (int index = 0; index < normalized.size(); index++) {
            fixed.add(normalized.get(index).withSelected(index == 0));
        }
        return List.copyOf(fixed);
    }

    private static Duration positive(Duration value, Duration fallback) {
        if (value == null || value.isZero() || value.isNegative()) {
            return fallback;
        }
        return value;
    }

    private static Optional<String> textSetting(String propertyName, String environmentName) {
        return EnvironmentSettings.text(propertyName, environmentName);
    }

    private static Optional<Double> numberSetting(String propertyName, String environmentName) {
        return textSetting(propertyName, environmentName).map(Double::parseDouble);
    }

    private static Optional<URI> uriSetting(String propertyName, String environmentName) {
        return textSetting(propertyName, environmentName).map(URI::create);
    }

    private static Duration secondsSetting(String propertyName, String environmentName, Duration fallback) {
        return numberSetting(propertyName, environmentName)
                .map(seconds -> Duration.ofMillis(Math.max(1L, Math.round(seconds * 1000.0))))
                .orElse(fallback);
    }

    private static int integerSetting(String propertyName, String environmentName, int fallback) {
        return textSetting(propertyName, environmentName)
                .map(Integer::parseInt)
                .orElse(fallback);
    }

    private static boolean booleanSetting(String propertyName, String environmentName, boolean fallback) {
        return textSetting(propertyName, environmentName)
                .map(value -> switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
                    case "1", "true", "yes", "on", "enabled" -> true;
                    case "0", "false", "no", "off", "disabled" -> false;
                    default -> fallback;
                })
                .orElse(fallback);
    }

    private static Optional<String> optional(String value) {
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static String trimmed(String value) {
        return value == null ? "" : value.trim();
    }
}
