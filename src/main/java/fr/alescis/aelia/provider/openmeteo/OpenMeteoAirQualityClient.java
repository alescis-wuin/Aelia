package fr.alescis.aelia.provider.openmeteo;

import java.util.Objects;

/**
 * Client for Open-Meteo air-quality and pollen requests.
 */
public final class OpenMeteoAirQualityClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoRequestFactory requestFactory;
    private final OpenMeteoHttpClient httpClient;

    public OpenMeteoAirQualityClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new OpenMeteoRequestFactory(configuration);
    }

    public OpenMeteoJsonValue fetchAirQuality(OpenMeteoLocation location) {
        return httpClient.getJson(requestFactory.airQualityUri(location), configuration.airQualityCacheTtl());
    }
}
