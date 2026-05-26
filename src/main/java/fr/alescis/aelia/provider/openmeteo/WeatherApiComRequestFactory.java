package fr.alescis.aelia.provider.openmeteo;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Builds WeatherAPI.com URIs only when the optional API key is configured.
 */
public final class WeatherApiComRequestFactory {
    private final OpenMeteoConfiguration configuration;

    public WeatherApiComRequestFactory(OpenMeteoConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public URI forecastUri(OpenMeteoLocation location) {
        Objects.requireNonNull(location, "location");
        String key = configuration.weatherApiKey()
                .orElseThrow(() -> new OpenMeteoException("WeatherAPI.com fallback requires a configured API key."));
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("key", key);
        parameters.put("q", location.latitude() + "," + location.longitude());
        parameters.put("days", "7");
        parameters.put("aqi", "yes");
        parameters.put("alerts", "yes");
        parameters.put("lang", "fr");
        return withQuery(configuration.weatherApiEndpoint(), parameters);
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
