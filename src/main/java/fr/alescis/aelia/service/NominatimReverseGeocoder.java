package fr.alescis.aelia.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Small reverse-geocoder for user-triggered map clicks.
 */
public final class NominatimReverseGeocoder implements AutoCloseable {
    public static final String USER_AGENT = "Aelia/0.4 (+https://github.com/alescis-wuin/Aelia)";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final long MINIMUM_INTERVAL_MILLIS = 1_100L;
    private static final String REVERSE_URL = System.getProperty("aelia.nominatim.reverseUrl", "https://nominatim.openstreetmap.org/reverse");

    private final ExecutorService executorService;
    private final HttpClient httpClient;
    private long lastRequestAt;

    public NominatimReverseGeocoder() {
        this.executorService = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "aelia-nominatim-" + ThreadIds.NEXT.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CompletableFuture<ReverseGeocodeResult> reverse(double latitude, double longitude) {
        validate(latitude, longitude);
        return CompletableFuture.supplyAsync(() -> reverseBlocking(latitude, longitude), executorService);
    }

    private ReverseGeocodeResult reverseBlocking(double latitude, double longitude) {
        try {
            waitForRateLimit();
            HttpRequest request = HttpRequest.newBuilder(requestUri(latitude, longitude))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Accept-Language", "fr")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Nominatim returned HTTP " + response.statusCode());
            }
            return NominatimJson.parse(response.body(), latitude, longitude);
        } catch (IOException exception) {
            throw new IllegalStateException("Reverse geocoding failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Reverse geocoding was interrupted.", exception);
        }
    }

    private synchronized void waitForRateLimit() throws InterruptedException {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestAt;
        if (lastRequestAt > 0 && elapsed < MINIMUM_INTERVAL_MILLIS) {
            Thread.sleep(MINIMUM_INTERVAL_MILLIS - elapsed);
        }
        lastRequestAt = System.currentTimeMillis();
    }

    private URI requestUri(double latitude, double longitude) {
        String uri = String.format(Locale.ROOT,
                REVERSE_URL + "?format=jsonv2&lat=%.7f&lon=%.7f&zoom=10&addressdetails=1&accept-language=fr",
                latitude,
                longitude
        );
        return URI.create(uri);
    }

    private static void validate(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90.");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180.");
        }
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }

    private static final class ThreadIds {
        private static final AtomicInteger NEXT = new AtomicInteger();

        private ThreadIds() {
        }
    }
}
