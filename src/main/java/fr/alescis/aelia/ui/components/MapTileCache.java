package fr.alescis.aelia.ui.components;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Small tile loader with a distinct User-Agent and a local minimum seven-day cache.
 */
final class MapTileCache implements AutoCloseable {
    static final double TILE_SIZE = 256.0;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(25);
    private static final Duration MINIMUM_CACHE_TTL = Duration.ofDays(7);
    private static final String USER_AGENT = System.getProperty(
            "aelia.map.userAgent",
            "Aelia/0.4.1 (+https://github.com/alescis-wuin/Aelia)"
    );

    private final String tileUrlTemplate;
    private final Path cacheRoot;
    private final ExecutorService executorService;
    private final HttpClient httpClient;
    private final Map<String, CompletableFuture<Path>> inFlight = new ConcurrentHashMap<>();
    private final Image placeholder = placeholderImage();

    MapTileCache(String tileUrlTemplate) {
        this.tileUrlTemplate = Objects.requireNonNull(tileUrlTemplate, "tileUrlTemplate");
        this.cacheRoot = defaultCacheRoot();
        this.executorService = Executors.newFixedThreadPool(3, new TileThreadFactory());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .executor(executorService)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    Image placeholder() {
        return placeholder;
    }

    void loadTile(int zoom, int tileX, int tileY, ImageView imageView, Consumer<String> failureHandler) {
        Objects.requireNonNull(imageView, "imageView");
        String key = key(zoom, tileX, tileY);
        Path cachedPath = cachePath(zoom, tileX, tileY);
        if (Files.isRegularFile(cachedPath)) {
            imageView.setImage(fileImage(cachedPath));
            if (!expired(cachedPath)) {
                return;
            }
        } else {
            imageView.setImage(placeholder);
        }

        CompletableFuture<Path> future = inFlight.computeIfAbsent(key, ignored -> CompletableFuture.supplyAsync(() -> {
            try {
                return fetchTile(zoom, tileX, tileY, cachedPath);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }, executorService).whenComplete((path, error) -> inFlight.remove(key)));

        future.whenComplete((path, error) -> {
            if (error == null && path != null && Files.isRegularFile(path)) {
                Platform.runLater(() -> imageView.setImage(fileImage(path)));
            } else if (!Files.isRegularFile(cachedPath) && failureHandler != null) {
                Platform.runLater(() -> failureHandler.accept("Tuiles indisponibles. Vérifiez la connexion ou changez de fournisseur de tuiles."));
            }
        });
    }

    private Path fetchTile(int zoom, int tileX, int tileY, Path targetPath) throws IOException {
        URI uri = URI.create(tileUrl(zoom, tileX, tileY));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Tile request interrupted", exception);
        }
        int status = response.statusCode();
        if (status != 200) {
            throw new IOException("Tile request failed with HTTP " + status + " for " + uri);
        }
        Files.createDirectories(targetPath.getParent());
        Path temporary = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
        Files.write(temporary, response.body());
        Files.move(temporary, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return targetPath;
    }

    private boolean expired(Path cachedPath) {
        try {
            Instant lastModified = Files.getLastModifiedTime(cachedPath).toInstant();
            return lastModified.plus(MINIMUM_CACHE_TTL).isBefore(Instant.now());
        } catch (IOException exception) {
            return true;
        }
    }

    private Path cachePath(int zoom, int tileX, int tileY) {
        return cacheRoot
                .resolve(Integer.toString(zoom))
                .resolve(Integer.toString(tileX))
                .resolve(tileY + ".png");
    }

    private Image fileImage(Path path) {
        return new Image(path.toUri().toString(), TILE_SIZE, TILE_SIZE, false, false, true);
    }

    private String tileUrl(int zoom, int tileX, int tileY) {
        return tileUrlTemplate
                .replace("{z}", Integer.toString(zoom))
                .replace("{x}", Integer.toString(tileX))
                .replace("{y}", Integer.toString(tileY));
    }

    private static String key(int zoom, int tileX, int tileY) {
        return zoom + "/" + tileX + "/" + tileY;
    }

    private static Path defaultCacheRoot() {
        String configured = System.getProperty("aelia.map.cache.dir");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim());
        }
        String xdgCache = System.getenv("XDG_CACHE_HOME");
        if (xdgCache != null && !xdgCache.isBlank()) {
            return Path.of(xdgCache).resolve("aelia").resolve("map-tiles");
        }
        return Path.of(System.getProperty("user.home", "."), ".cache", "aelia", "map-tiles");
    }

    private static Image placeholderImage() {
        WritableImage image = new WritableImage((int) TILE_SIZE, (int) TILE_SIZE);
        PixelWriter writer = image.getPixelWriter();
        Color background = Color.web("#0B1220");
        Color grid = Color.web("#101B31");
        for (int y = 0; y < (int) TILE_SIZE; y++) {
            for (int x = 0; x < (int) TILE_SIZE; x++) {
                boolean gridLine = x % 64 == 0 || y % 64 == 0;
                writer.setColor(x, y, gridLine ? grid : background);
            }
        }
        return image;
    }

    @Override
    public void close() {
        inFlight.clear();
        executorService.shutdownNow();
    }

    private static final class TileThreadFactory implements ThreadFactory {
        private final AtomicInteger nextId = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "aelia-map-tile-" + nextId.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
