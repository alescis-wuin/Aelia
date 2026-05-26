package fr.alescis.aelia.provider.openmeteo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Optional Weatherbit fallback client using free current and 7-day daily forecast endpoints.
 */
public final class WeatherbitForecastClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoHttpClient httpClient;
    private final WeatherbitRequestFactory requestFactory;

    public WeatherbitForecastClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new WeatherbitRequestFactory(configuration);
    }

    public boolean enabled() {
        return configuration.weatherbitEnabled();
    }

    public OpenMeteoJsonValue fetchForecast(OpenMeteoLocation location) {
        if (!enabled()) {
            throw new OpenMeteoException("Weatherbit fallback is disabled or no API key is configured.");
        }
        OpenMeteoJsonValue current = httpClient.getJson(requestFactory.currentUri(location), configuration.weatherbitCacheTtl());
        OpenMeteoJsonValue daily = httpClient.getJson(requestFactory.dailyForecastUri(location), configuration.weatherbitCacheTtl());
        Map<String, OpenMeteoJsonValue> combined = new LinkedHashMap<>();
        combined.put("current", current);
        combined.put("daily", daily);
        return JsonValueBuilder.object(combined);
    }
}
