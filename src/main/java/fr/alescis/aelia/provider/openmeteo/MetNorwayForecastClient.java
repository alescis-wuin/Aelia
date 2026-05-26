package fr.alescis.aelia.provider.openmeteo;

import java.util.Objects;

/**
 * Client for MET Norway Locationforecast, used as a no-key forecast fallback.
 */
public final class MetNorwayForecastClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoRequestFactory requestFactory;
    private final OpenMeteoHttpClient httpClient;

    public MetNorwayForecastClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new OpenMeteoRequestFactory(configuration);
    }

    public OpenMeteoJsonValue fetchForecast(OpenMeteoLocation location) {
        if (!configuration.metNorwayEnabled()) {
            throw new OpenMeteoException("MET Norway fallback is disabled by configuration.");
        }
        return httpClient.getJson(requestFactory.metNorwayForecastUri(location), configuration.metNorwayCacheTtl());
    }
}
