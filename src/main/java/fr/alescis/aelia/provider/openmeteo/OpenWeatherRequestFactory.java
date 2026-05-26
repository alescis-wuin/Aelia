package fr.alescis.aelia.provider.openmeteo;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Builds OpenWeather free current and 5-day forecast requests.
 */
public final class OpenWeatherRequestFactory {
    private final OpenMeteoConfiguration configuration;

    public OpenWeatherRequestFactory(OpenMeteoConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public URI currentWeatherUri(OpenMeteoLocation location) {
        Map<String, String> parameters = baseParameters(location);
        return withQuery(configuration.openWeatherCurrentEndpoint(), parameters);
    }

    public URI forecast5Uri(OpenMeteoLocation location) {
        Map<String, String> parameters = baseParameters(location);
        return withQuery(configuration.openWeatherForecastEndpoint(), parameters);
    }

    private Map<String, String> baseParameters(OpenMeteoLocation location) {
        Objects.requireNonNull(location, "location");
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("lat", Double.toString(location.latitude()));
        parameters.put("lon", Double.toString(location.longitude()));
        parameters.put("units", "metric");
        parameters.put("lang", "fr");
        parameters.put("appid", configuration.openWeatherApiKey()
                .orElseThrow(() -> new OpenMeteoException("OpenWeather fallback has no API key configured.")));
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
