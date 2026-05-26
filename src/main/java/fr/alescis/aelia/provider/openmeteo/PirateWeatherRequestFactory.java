package fr.alescis.aelia.provider.openmeteo;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Builds Pirate Weather forecast requests.
 */
public final class PirateWeatherRequestFactory {
    private final OpenMeteoConfiguration configuration;

    public PirateWeatherRequestFactory(OpenMeteoConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public URI forecastUri(OpenMeteoLocation location) {
        String apiKey = configuration.pirateWeatherApiKey()
                .orElseThrow(() -> new OpenMeteoException("Pirate Weather fallback has no API key configured."));
        String base = trimTrailingSlash(configuration.pirateWeatherEndpoint().toString())
                + "/" + encodePath(apiKey) + "/" + location.latitude() + "," + location.longitude();
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("units", "ca");
        parameters.put("exclude", "minutely,alerts");
        parameters.put("lang", "fr");
        parameters.put("version", "2");
        return withQuery(URI.create(base), parameters);
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

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodePath(String value) {
        return encode(value).replace("%2C", ",");
    }
}
