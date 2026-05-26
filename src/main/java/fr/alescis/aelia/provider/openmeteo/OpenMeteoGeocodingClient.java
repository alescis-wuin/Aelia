package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.model.WeatherCondition;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Client for Open-Meteo city search. It is intentionally independent from the dashboard UI.
 */
public final class OpenMeteoGeocodingClient {
    private final OpenMeteoConfiguration configuration;
    private final OpenMeteoRequestFactory requestFactory;
    private final OpenMeteoHttpClient httpClient;

    public OpenMeteoGeocodingClient(OpenMeteoConfiguration configuration, OpenMeteoHttpClient httpClient) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestFactory = new OpenMeteoRequestFactory(configuration);
    }

    public List<OpenMeteoLocation> search(String query, int count) {
        OpenMeteoJsonValue payload = httpClient.getJson(
                requestFactory.geocodingSearchUri(query, count),
                configuration.geocodingCacheTtl()
        );
        return payload.get("results").asArray().stream()
                .map(this::toLocation)
                .toList();
    }

    private OpenMeteoLocation toLocation(OpenMeteoJsonValue result) {
        String city = result.get("name").asString("Localisation");
        String country = result.get("country").asString(result.get("country_code").asString("Pays"));
        double latitude = result.get("latitude").asDouble(0.0);
        double longitude = result.get("longitude").asDouble(0.0);
        ZoneId zoneId = ZoneId.of(result.get("timezone").asString(ZoneId.systemDefault().getId()));
        return new OpenMeteoLocation(city, country, latitude, longitude, zoneId, WeatherCondition.CLOUDY, 0, false);
    }
}
