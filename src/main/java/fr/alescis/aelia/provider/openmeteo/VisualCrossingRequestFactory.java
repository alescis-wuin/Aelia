package fr.alescis.aelia.provider.openmeteo;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Builds Visual Crossing Timeline Weather API requests.
 */
public final class VisualCrossingRequestFactory {
    private final OpenMeteoConfiguration configuration;

    public VisualCrossingRequestFactory(OpenMeteoConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public URI timelineUri(OpenMeteoLocation location) {
        LocalDate startDate = LocalDate.now(location.zoneId());
        LocalDate endDate = startDate.plusDays(6);
        String locationPath = encodePath(location.latitude() + "," + location.longitude());
        String base = trimTrailingSlash(configuration.visualCrossingTimelineEndpoint().toString())
                + "/" + locationPath + "/" + startDate + "/" + endDate;
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("unitGroup", "metric");
        parameters.put("include", "current,hours,days,alerts");
        parameters.put("contentType", "json");
        parameters.put("key", configuration.visualCrossingApiKey()
                .orElseThrow(() -> new OpenMeteoException("Visual Crossing fallback has no API key configured.")));
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
