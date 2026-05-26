package fr.alescis.aelia.provider.openmeteo;

import java.net.URI;
import java.time.LocalDate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Builds all Open-Meteo URIs from strongly named request methods.
 */
public final class OpenMeteoRequestFactory {
    private static final String FULL_CURRENT = String.join(",",
            "temperature_2m",
            "relative_humidity_2m",
            "apparent_temperature",
            "precipitation",
            "rain",
            "showers",
            "snowfall",
            "weather_code",
            "cloud_cover",
            "pressure_msl",
            "wind_speed_10m",
            "wind_direction_10m",
            "wind_gusts_10m",
            "is_day"
    );

    private static final String FULL_HOURLY = String.join(",",
            "temperature_2m",
            "weather_code",
            "precipitation_probability",
            "pressure_msl"
    );

    private static final String FULL_DAILY = String.join(",",
            "weather_code",
            "temperature_2m_max",
            "temperature_2m_min",
            "sunrise",
            "sunset",
            "daylight_duration",
            "uv_index_max",
            "precipitation_probability_max",
            "wind_speed_10m_max",
            "wind_direction_10m_dominant"
    );

    private static final String SUMMARY_CURRENT = String.join(",",
            "temperature_2m",
            "weather_code",
            "is_day"
    );

    private static final String REDUCED_CURRENT = String.join(",",
            "temperature_2m",
            "relative_humidity_2m",
            "apparent_temperature",
            "weather_code",
            "pressure_msl",
            "wind_speed_10m",
            "wind_direction_10m",
            "wind_gusts_10m",
            "is_day"
    );

    private static final String REDUCED_HOURLY = String.join(",",
            "temperature_2m",
            "weather_code",
            "precipitation_probability",
            "pressure_msl"
    );

    private static final String REDUCED_DAILY = String.join(",",
            "weather_code",
            "temperature_2m_max",
            "temperature_2m_min",
            "sunrise",
            "sunset",
            "daylight_duration",
            "uv_index_max",
            "precipitation_probability_max",
            "wind_speed_10m_max",
            "wind_direction_10m_dominant"
    );


    private static final String ARCHIVE_HOURLY = String.join(",",
            "temperature_2m",
            "relative_humidity_2m",
            "apparent_temperature",
            "precipitation",
            "weather_code",
            "pressure_msl",
            "wind_speed_10m",
            "wind_direction_10m",
            "wind_gusts_10m",
            "is_day"
    );

    private static final String ARCHIVE_DAILY = String.join(",",
            "weather_code",
            "temperature_2m_max",
            "temperature_2m_min",
            "sunrise",
            "sunset",
            "daylight_duration",
            "precipitation_sum",
            "wind_speed_10m_max",
            "wind_direction_10m_dominant"
    );

    private static final String AIR_HOURLY = String.join(",",
            "pm10",
            "pm2_5",
            "nitrogen_dioxide",
            "uv_index",
            "european_aqi",
            "grass_pollen",
            "birch_pollen",
            "olive_pollen",
            "ragweed_pollen"
    );

    private final OpenMeteoConfiguration configuration;

    public OpenMeteoRequestFactory(OpenMeteoConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public URI fullForecastUri(OpenMeteoLocation location) {
        Map<String, String> parameters = baseWeatherParameters(location);
        parameters.put("current", FULL_CURRENT);
        parameters.put("hourly", FULL_HOURLY);
        parameters.put("daily", FULL_DAILY);
        parameters.put("forecast_days", "7");
        parameters.put("temperature_unit", "celsius");
        parameters.put("wind_speed_unit", "kmh");
        parameters.put("precipitation_unit", "mm");
        return withQuery(configuration.forecastEndpoint(), parameters);
    }


    public URI reducedForecastUri(OpenMeteoLocation location) {
        Map<String, String> parameters = baseWeatherParameters(location);
        parameters.put("current", REDUCED_CURRENT);
        parameters.put("hourly", REDUCED_HOURLY);
        parameters.put("daily", REDUCED_DAILY);
        parameters.put("forecast_days", "7");
        parameters.put("temperature_unit", "celsius");
        parameters.put("wind_speed_unit", "kmh");
        parameters.put("precipitation_unit", "mm");
        return withQuery(configuration.forecastEndpoint(), parameters);
    }

    public URI summaryForecastUri(OpenMeteoLocation location) {
        Map<String, String> parameters = baseWeatherParameters(location);
        parameters.put("current", SUMMARY_CURRENT);
        parameters.put("forecast_days", "1");
        parameters.put("temperature_unit", "celsius");
        parameters.put("wind_speed_unit", "kmh");
        parameters.put("precipitation_unit", "mm");
        return withQuery(configuration.forecastEndpoint(), parameters);
    }


    public URI archiveFallbackUri(OpenMeteoLocation location) {
        LocalDate endDate = LocalDate.now(location.zoneId()).minusDays(configuration.archiveFallbackLagDays());
        LocalDate startDate = endDate.minusDays(6);
        Map<String, String> parameters = baseWeatherParameters(location);
        parameters.put("start_date", startDate.toString());
        parameters.put("end_date", endDate.toString());
        parameters.put("hourly", ARCHIVE_HOURLY);
        parameters.put("daily", ARCHIVE_DAILY);
        parameters.put("temperature_unit", "celsius");
        parameters.put("wind_speed_unit", "kmh");
        parameters.put("precipitation_unit", "mm");
        return withQuery(configuration.archiveEndpoint(), parameters);
    }

    public URI airQualityUri(OpenMeteoLocation location) {
        Map<String, String> parameters = baseWeatherParameters(location);
        parameters.put("hourly", AIR_HOURLY);
        parameters.put("forecast_days", "4");
        return withQuery(configuration.airQualityEndpoint(), parameters);
    }


    public URI metNorwayForecastUri(OpenMeteoLocation location) {
        Objects.requireNonNull(location, "location");
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("lat", truncatedCoordinate(location.latitude()));
        parameters.put("lon", truncatedCoordinate(location.longitude()));
        return withQuery(configuration.metNorwayEndpoint(), parameters);
    }

    public URI geocodingSearchUri(String query, int count) {
        String normalizedQuery = Objects.requireNonNull(query, "query").trim();
        if (normalizedQuery.length() < 2) {
            throw new IllegalArgumentException("Geocoding query must contain at least two characters.");
        }
        int normalizedCount = Math.max(1, Math.min(100, count));
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("name", normalizedQuery);
        parameters.put("count", String.valueOf(normalizedCount));
        parameters.put("language", "fr");
        parameters.put("format", "json");
        configuration.apiKey().ifPresent(apiKey -> parameters.put("apikey", apiKey));
        return withQuery(configuration.geocodingEndpoint(), parameters);
    }

    private Map<String, String> baseWeatherParameters(OpenMeteoLocation location) {
        Objects.requireNonNull(location, "location");
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("latitude", Double.toString(location.latitude()));
        parameters.put("longitude", Double.toString(location.longitude()));
        parameters.put("timezone", location.zoneId().getId());
        configuration.apiKey().ifPresent(apiKey -> parameters.put("apikey", apiKey));
        return parameters;
    }

    private static String truncatedCoordinate(double value) {
        return String.format(Locale.ROOT, "%.4f", value);
    }

    private static URI withQuery(URI endpoint, Map<String, String> parameters) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            joiner.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        String base = endpoint.toString();
        String separator = base.contains("?") ? "&" : "?";
        return URI.create(base + separator + joiner);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
