package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.provider.ProviderDiagnostics;

import java.util.Objects;

/**
 * Client for Open-Meteo forecast requests.
 */
public final class OpenMeteoForecastClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoRequestFactory requestFactory;
    private final OpenMeteoHttpClient httpClient;

    public OpenMeteoForecastClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new OpenMeteoRequestFactory(configuration);
    }

    public OpenMeteoJsonValue fetchFullForecast(OpenMeteoLocation location) {
        try {
            return httpClient.getJson(requestFactory.fullForecastUri(location), configuration.forecastCacheTtl());
        } catch (OpenMeteoException exception) {
            if (!exception.transientFailure()) {
                throw exception;
            }
            ProviderDiagnostics.warn(
                    "Full Open-Meteo forecast request failed with a transient gateway or transport error. Retrying with a reduced variable set.",
                    exception
            );
            try {
                return httpClient.getJson(requestFactory.reducedForecastUri(location), configuration.forecastCacheTtl());
            } catch (OpenMeteoException reducedException) {
                reducedException.addSuppressed(exception);
                throw reducedException;
            }
        }
    }

    public OpenMeteoJsonValue fetchSummaryForecast(OpenMeteoLocation location) {
        return httpClient.getJson(requestFactory.summaryForecastUri(location), configuration.forecastCacheTtl());
    }
}
