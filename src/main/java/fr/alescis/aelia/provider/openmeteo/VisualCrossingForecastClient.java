package fr.alescis.aelia.provider.openmeteo;

import java.util.Objects;

/**
 * Optional Visual Crossing Timeline Weather API fallback client.
 */
public final class VisualCrossingForecastClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoHttpClient httpClient;
    private final VisualCrossingRequestFactory requestFactory;

    public VisualCrossingForecastClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new VisualCrossingRequestFactory(configuration);
    }

    public boolean enabled() {
        return configuration.visualCrossingEnabled();
    }

    public OpenMeteoJsonValue fetchForecast(OpenMeteoLocation location) {
        if (!enabled()) {
            throw new OpenMeteoException("Visual Crossing fallback is disabled or no API key is configured.");
        }
        return httpClient.getJson(requestFactory.timelineUri(location), configuration.visualCrossingCacheTtl());
    }
}
