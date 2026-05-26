package fr.alescis.aelia.provider.openmeteo;

import fr.alescis.aelia.provider.ProviderDiagnostics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small cached HTTP client dedicated to JSON Open-Meteo endpoints.
 */
public final class OpenMeteoHttpClient implements AutoCloseable {
    private final HttpClient client;
    private final Clock clock;
    private final String userAgent;
    private final Duration requestTimeout;
    private final Map<URI, CachedResponse> cache = new ConcurrentHashMap<>();

    public OpenMeteoHttpClient(OpenMeteoConfiguration configuration) {
        this(configuration, Clock.systemUTC());
    }

    OpenMeteoHttpClient(OpenMeteoConfiguration configuration, Clock clock) {
        Objects.requireNonNull(configuration, "configuration");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.userAgent = configuration.userAgent();
        this.requestTimeout = configuration.requestTimeout();
        this.client = HttpClient.newBuilder()
                .connectTimeout(configuration.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        ProviderDiagnostics.info("Open-Meteo HTTP client configured with connect timeout "
                + configuration.connectTimeout().toSeconds() + "s and request timeout "
                + requestTimeout.toSeconds() + "s.");
    }

    public OpenMeteoJsonValue getJson(URI uri, Duration timeToLive) {
        URI normalizedUri = Objects.requireNonNull(uri, "uri");
        Duration ttl = timeToLive == null || timeToLive.isNegative() ? Duration.ZERO : timeToLive;
        Instant now = clock.instant();
        CachedResponse cached = cache.get(normalizedUri);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            ProviderDiagnostics.info("Open-Meteo cache hit for " + redacted(normalizedUri));
            return OpenMeteoJsonParser.parse(cached.body());
        }
        String body = fetch(normalizedUri);
        cache.put(normalizedUri, new CachedResponse(body, now.plus(ttl)));
        return OpenMeteoJsonParser.parse(body);
    }

    public void clearCache() {
        cache.clear();
    }

    @Override
    public void close() {
        clearCache();
    }

    private String fetch(URI uri) {
        ProviderDiagnostics.info("Open-Meteo GET " + redacted(uri));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                ProviderDiagnostics.info("Open-Meteo HTTP " + statusCode + " for " + redacted(uri));
                return response.body();
            }
            if (statusCode == 429) {
                throw new OpenMeteoException("Open-Meteo rate limit reached for " + redacted(uri) + ".", statusCode, false);
            }
            boolean transientFailure = statusCode == 408 || statusCode >= 500;
            throw new OpenMeteoException("Open-Meteo request failed with HTTP " + statusCode + " for "
                    + redacted(uri) + ": " + trimBody(response.body()) + ".", statusCode, transientFailure);
        } catch (IOException exception) {
            throw new OpenMeteoException("Open-Meteo request failed for " + redacted(uri) + ".", exception, -1, true);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenMeteoException("Open-Meteo request was interrupted for " + redacted(uri) + ".", exception);
        }
    }

    private static String redacted(URI uri) {
        String value = uri.toASCIIString();
        value = value.replaceAll("(?i)([?&](?:apikey|api_key)=)[^&]*", "$1***");
        return value;
    }

    private static String trimBody(String body) {
        if (body == null || body.isBlank()) {
            return "empty response";
        }
        String normalized = body.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }

    private record CachedResponse(String body, Instant expiresAt) {
    }
}
