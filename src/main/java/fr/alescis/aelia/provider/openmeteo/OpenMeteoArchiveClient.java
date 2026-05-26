package fr.alescis.aelia.provider.openmeteo;

import java.util.Objects;

/**
 * Client for Open-Meteo Historical Weather API requests used as a partial fallback.
 */
public final class OpenMeteoArchiveClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoRequestFactory requestFactory;
    private final OpenMeteoHttpClient httpClient;

    public OpenMeteoArchiveClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new OpenMeteoRequestFactory(configuration);
    }

    public OpenMeteoJsonValue fetchArchiveFallback(OpenMeteoLocation location) {
        return httpClient.getJson(requestFactory.archiveFallbackUri(location), configuration.archiveCacheTtl());
    }
}
