package fr.alescis.aelia.provider.openmeteo;

import java.util.Objects;

/**
 * Optional Pirate Weather fallback client using Dark-Sky-compatible forecast payloads.
 */
public final class PirateWeatherForecastClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoHttpClient httpClient;
    private final PirateWeatherRequestFactory requestFactory;

    public PirateWeatherForecastClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new PirateWeatherRequestFactory(configuration);
    }

    public boolean enabled() {
        return configuration.pirateWeatherEnabled();
    }

    public OpenMeteoJsonValue fetchForecast(OpenMeteoLocation location) {
        if (!enabled()) {
            throw new OpenMeteoException("Pirate Weather fallback is disabled or no API key is configured.");
        }
        return httpClient.getJson(requestFactory.forecastUri(location), configuration.pirateWeatherCacheTtl());
    }
}
