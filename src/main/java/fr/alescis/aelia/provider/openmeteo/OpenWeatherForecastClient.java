package fr.alescis.aelia.provider.openmeteo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Optional OpenWeather fallback based on free current weather and 5-day / 3-hour forecast endpoints.
 */
public final class OpenWeatherForecastClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoHttpClient httpClient;
    private final OpenWeatherRequestFactory requestFactory;

    public OpenWeatherForecastClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new OpenWeatherRequestFactory(configuration);
    }

    public boolean enabled() {
        return configuration.openWeatherEnabled();
    }

    public OpenMeteoJsonValue fetchForecast(OpenMeteoLocation location) {
        if (!enabled()) {
            throw new OpenMeteoException("OpenWeather fallback is disabled or no API key is configured.");
        }
        OpenMeteoJsonValue current = httpClient.getJson(requestFactory.currentWeatherUri(location), configuration.openWeatherCacheTtl());
        OpenMeteoJsonValue forecast = httpClient.getJson(requestFactory.forecast5Uri(location), configuration.openWeatherCacheTtl());
        Map<String, OpenMeteoJsonValue> combined = new LinkedHashMap<>();
        combined.put("currentWeather", current);
        combined.put("forecast5", forecast);
        return JsonValueBuilder.object(combined);
    }
}
