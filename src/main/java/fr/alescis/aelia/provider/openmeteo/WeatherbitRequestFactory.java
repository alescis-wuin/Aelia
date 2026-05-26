package fr.alescis.aelia.provider.openmeteo;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Builds Weatherbit current and daily forecast requests.
 */
public final class WeatherbitRequestFactory {
    private final OpenMeteoConfiguration configuration;

    public WeatherbitRequestFactory(OpenMeteoConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public URI currentUri(OpenMeteoLocation location) {
        Map<String, String> parameters = baseParameters(location);
        return withQuery(configuration.weatherbitCurrentEndpoint(), parameters);
    }

    public URI dailyForecastUri(OpenMeteoLocation location) {
        Map<String, String> parameters = baseParameters(location);
        parameters.put("days", "7");
        return withQuery(configuration.weatherbitDailyEndpoint(), parameters);
    }

    private Map<String, String> baseParameters(OpenMeteoLocation location) {
        Objects.requireNonNull(location, "location");
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("lat", Double.toString(location.latitude()));
        parameters.put("lon", Double.toString(location.longitude()));
        parameters.put("units", "M");
        parameters.put("lang", "fr");
        parameters.put("key", configuration.weatherbitApiKey()
                .orElseThrow(() -> new OpenMeteoException("Weatherbit fallback has no API key configured.")));
        return parameters;
    }

    private static URI withQuery(URI endpoint, Map<String, String> parameters) {
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            joiner.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        String base = endpoint.toString();
        String separator = base.contains("?") ? "&" : "?";
        return URI.create(base + separator + joiner);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
