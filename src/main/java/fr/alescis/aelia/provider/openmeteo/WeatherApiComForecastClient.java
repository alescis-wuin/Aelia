package fr.alescis.aelia.provider.openmeteo;

import java.util.Objects;

/**
 * Optional WeatherAPI.com fallback client. It remains inactive until an API key is configured.
 */
public final class WeatherApiComForecastClient {
    private final OpenMeteoConfiguration configuration;
    private final WeatherApiComRequestFactory requestFactory;
    private final OpenMeteoHttpClient httpClient;

    public WeatherApiComForecastClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new WeatherApiComRequestFactory(configuration);
    }

    public boolean enabled() {
        return configuration.weatherApiEnabled();
    }

    public OpenMeteoJsonValue fetchForecast(OpenMeteoLocation location) {
        if (!enabled()) {
            throw new OpenMeteoException("WeatherAPI.com fallback is disabled or no API key is configured.");
        }
        return httpClient.getJson(requestFactory.forecastUri(location), configuration.weatherApiCacheTtl());
    }
}
