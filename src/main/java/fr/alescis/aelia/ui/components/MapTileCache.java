package fr.alescis.aelia.ui.components;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Tile loader with viewport scheduling, cache diagnostics, image validation and explicit HTTP logging.
 */
final class MapTileCache implements AutoCloseable {
    static final double TILE_SIZE = 256.0;

    private static final System.Logger LOGGER = System.getLogger(MapTileCache.class.getName());
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(25);
    private static final Duration MINIMUM_CACHE_TTL = Duration.ofDays(7);
    private static final Duration CACHE_INSPECTION_INTERVAL = Duration.ofSeconds(10);
    private static final long DEFAULT_REQUEST_INTERVAL_MILLIS = 350L;
    private static final long DEFAULT_INACTIVE_TILE_GRACE_MILLIS = 2_500L;
    private static final long DEFAULT_FAILURE_RETRY_MILLIS = 5_000L;
    private static final long DEFAULT_MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final int DEFAULT_MAX_IMAGE_DIMENSION = 1024;
    private static final int DEFAULT_MINIMUM_DISTINCT_COLORS = 2;
    private static final String USER_AGENT = System.getProperty(
            "aelia.map.userAgent",
            "Aelia/0.4.1 (+https://github.com/alescis-wuin/Aelia; contact: alescis-wuin)"
    );

    private final MapTileProvider provider;
    private final Path cacheRoot;
    private final ExecutorService tileExecutor;
    private final HttpClient httpClient;
    private final PriorityBlockingQueue<TileWorkItem> workQueue = new PriorityBlockingQueue<>();
    private final java.util.Map<String, TileState> tileStates = new ConcurrentHashMap<>();
    private final Set<String> activeTileKeys = ConcurrentHashMap.newKeySet();
    private final Image placeholder = placeholderImage();
    private final long requestIntervalMillis;
    private final long inactiveTileGraceMillis;
    private final long failureRetryMillis;
    private final long maxImageBytes;
    private final int maxImageDimension;
    private final int minimumDistinctColors;
    private final boolean diagnosticsEnabled;
    private final AtomicLong workSequence = new AtomicLong();
    private final Object rateLimitLock = new Object();
    private volatile boolean closed;
    private volatile long lastCacheInspectionAtMillis;
    private long nextRequestAtMillis;

    MapTileCache(MapTileProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.cacheRoot = defaultCacheRoot().resolve(provider.id());
        this.requestIntervalMillis = configuredLong("aelia.map.tileRequestIntervalMillis", DEFAULT_REQUEST_INTERVAL_MILLIS, 100L, 60_000L);
        this.inactiveTileGraceMillis = configuredLong("aelia.map.inactiveTileGraceMillis", DEFAULT_INACTIVE_TILE_GRACE_MILLIS, 0L, 60_000L);
        this.failureRetryMillis = configuredLong("aelia.map.failureRetryMillis", DEFAULT_FAILURE_RETRY_MILLIS, 0L, 300_000L);
        this.maxImageBytes = configuredLong("aelia.map.maxImageBytes", DEFAULT_MAX_IMAGE_BYTES, 1L, 50L * 1024L * 1024L);
        this.maxImageDimension = (int) configuredLong("aelia.map.maxImageDimension", DEFAULT_MAX_IMAGE_DIMENSION, 1L, 16_384L);
        this.minimumDistinctColors = (int) configuredLong("aelia.map.minimumDistinctColors", DEFAULT_MINIMUM_DISTINCT_COLORS, 1L, 256L);
        this.diagnosticsEnabled = configuredBoolean("aelia.map.diagnostics", true);
        this.tileExecutor = Executors.newSingleThreadExecutor(new TileThreadFactory());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)
                .build();
        logInfo("Initialisation des tuiles: provider=" + provider.displayName()
                + ", template=" + provider.urlTemplate()
                + ", cache=" + cacheRoot
                + ", requestIntervalMillis=" + requestIntervalMillis
                + ", inactiveTileGraceMillis=" + inactiveTileGraceMillis
                + ", failureRetryMillis=" + failureRetryMillis
                + ", maxImageBytes=" + maxImageBytes
                + ", maxImageDimension=" + maxImageDimension
                + ", minimumDistinctColors=" + minimumDistinctColors
                + ", userAgent=" + USER_AGENT);
        inspectCacheRoot(true);
        tileExecutor.execute(this::runWorkerLoop);
    }

    Image placeholder() {
        return placeholder;
    }

    MapTileProvider provider() {
        return provider;
    }

    String diagnosticsSummary() {
        return "Tuiles " + provider.displayName()
                + " · cache " + cacheRoot
                + " · délai " + requestIntervalMillis + " ms";
    }

    void setActiveTiles(Set<String> keys) {
        activeTileKeys.clear();
        activeTileKeys.addAll(keys);
        inspectCacheRoot(false);
        cleanupDormantTileStates();
    }

    String key(int zoom, int tileX, int tileY) {
        return zoom + "/" + tileX + "/" + tileY;
    }

    void loadTile(int zoom, int tileX, int tileY, ImageView imageView, Consumer<String> statusHandler) {
        loadTile(zoom, tileX, tileY, Integer.MAX_VALUE / 2, imageView, statusHandler);
    }

    void loadTile(int zoom, int tileX, int tileY, int priority, ImageView imageView, Consumer<String> statusHandler) {
        Objects.requireNonNull(imageView, "imageView");
        String key = key(zoom, tileX, tileY);
        Path cachedPath = cachePath(zoom, tileX, tileY);
        TileState state = tileStates.computeIfAbsent(key, ignored -> new TileState());
        state.addImageView(imageView);
        long version = state.markVisible(priority, statusHandler);

        if (Files.isRegularFile(cachedPath)) {
            imageView.setImage(fileImage(cachedPath, key, statusHandler));
            if (!expired(cachedPath)) {
                return;
            }
            logDebug("Cache expiré: key=" + key + ", cache=" + cachedPath);
        } else {
            imageView.setImage(placeholder);
        }

        if (!isRetryAllowed(state)) {
            logDebug("Chargement différé après échec récent: key=" + key + ", cache=" + cachedPath);
            return;
        }

        state.queuedVersion = version;
        workQueue.offer(new TileWorkItem(
                key,
                zoom,
                tileX,
                tileY,
                cachedPath,
                Math.max(0, priority),
                version,
                System.currentTimeMillis(),
                workSequence.incrementAndGet()
        ));
    }

    private void runWorkerLoop() {
        while (!closed && !Thread.currentThread().isInterrupted()) {
            try {
                TileWorkItem item = workQueue.take();
                processWorkItem(item);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException exception) {
                logError("Erreur inattendue du worker de tuiles.", exception);
            }
        }
    }

    private void processWorkItem(TileWorkItem item) {
        TileState state = tileStates.get(item.key());
        if (state == null || item.version() < state.version.get()) {
            return;
        }
        if (state.inFlight) {
            return;
        }
        if (!isStillWorthLoading(item.key(), state)) {
            logDebug("Chargement abandonné après stabilisation hors viewport: key=" + item.key()
                    + ", inactiveForMillis=" + inactiveForMillis(state));
            return;
        }
        if (!isRetryAllowed(state)) {
            return;
        }

        state.inFlight = true;
        try {
            Path path = loadTilePath(item, state);
            if (path != null && Files.isRegularFile(path)) {
                updateRegisteredViews(item.key(), path);
            }
        } catch (TileLoadException exception) {
            state.lastFailureAtMillis = System.currentTimeMillis();
            reportFailure(item, state, exception);
        } catch (RuntimeException exception) {
            state.lastFailureAtMillis = System.currentTimeMillis();
            reportFailure(item, state, new TileLoadException(
                    "Erreur inattendue pendant le chargement d'une tuile. Voir la console.",
                    "Erreur inattendue: key=" + item.key() + ", cache=" + item.cachedPath(),
                    exception
            ));
        } finally {
            state.inFlight = false;
        }
    }

    private Path loadTilePath(TileWorkItem item, TileState state) throws TileLoadException {
        if (Files.isRegularFile(item.cachedPath())) {
            try {
                validateImageFile(item.cachedPath(), item.key(), "cache");
                if (!expired(item.cachedPath())) {
                    logDebug("Tuile servie depuis le cache: key=" + item.key() + ", cache=" + item.cachedPath());
                    return item.cachedPath();
                }
            } catch (TileLoadException exception) {
                deleteInvalidCacheFile(item.cachedPath(), exception);
            }
        }

        if (!isStillWorthLoading(item.key(), state)) {
            logDebug("Chargement réseau évité après stabilisation hors viewport: key=" + item.key()
                    + ", inactiveForMillis=" + inactiveForMillis(state));
            return null;
        }
        return fetchTile(item, state);
    }

    private Path fetchTile(TileWorkItem item, TileState state) throws TileLoadException {
        throttle();
        if (!isStillWorthLoading(item.key(), state)) {
            logDebug("Chargement réseau annulé après temporisation: key=" + item.key()
                    + ", inactiveForMillis=" + inactiveForMillis(state));
            return null;
        }

        URI uri = URI.create(tileUrl(item.zoom(), item.tileX(), item.tileY()));
        HttpRequest request = buildRequest(uri, item.cachedPath());
        long start = System.nanoTime();
        logDebug("Requête tuile: key=" + item.key()
                + ", url=" + uri
                + ", cache=" + item.cachedPath()
                + ", priority=" + item.priority());

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException exception) {
            throw new TileLoadException(
                    "Erreur réseau pendant le chargement des tuiles. Voir la console.",
                    "Erreur réseau: key=" + item.key() + ", url=" + uri + ", cache=" + item.cachedPath(),
                    exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TileLoadException(
                    "Chargement de tuile interrompu.",
                    "Interruption: key=" + item.key() + ", url=" + uri + ", cache=" + item.cachedPath(),
                    exception
            );
        }

        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
        HttpDiagnostics httpDiagnostics = HttpDiagnostics.from(uri, item.cachedPath(), response, elapsedMillis);
        logHttpDiagnostics(item.key(), httpDiagnostics);

        int status = response.statusCode();
        if (status == 304 && Files.isRegularFile(item.cachedPath())) {
            TileImageDiagnostics imageDiagnostics = validateImageFile(item.cachedPath(), item.key(), "cache-304");
            logImageDiagnostics(item.key(), item.cachedPath(), imageDiagnostics, "cache-304");
            return item.cachedPath();
        }
        if (status != 200) {
            throw httpStatusException(item.key(), httpDiagnostics);
        }

        byte[] body = response.body();
        validateContentType(item.key(), httpDiagnostics.contentType(), uri, item.cachedPath());
        TileImageDiagnostics imageDiagnostics = validateImageBytes(body, item.key(), uri, item.cachedPath());
        logImageDiagnostics(item.key(), item.cachedPath(), imageDiagnostics, "download");

        try {
            Files.createDirectories(item.cachedPath().getParent());
            Path temporary = item.cachedPath().resolveSibling(item.cachedPath().getFileName() + ".tmp");
            Files.write(temporary, body);
            Files.move(temporary, item.cachedPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            long fileSize = Files.size(item.cachedPath());
            logDebug("Tuile écrite dans le cache: key=" + item.key()
                    + ", cache=" + item.cachedPath()
                    + ", fileSize=" + fileSize);
        } catch (IOException exception) {
            throw new TileLoadException(
                    "Impossible d'écrire une tuile dans le cache. Voir la console.",
                    "Erreur d'écriture cache: key=" + item.key() + ", cache=" + item.cachedPath(),
                    exception
            );
        }
        return item.cachedPath();
    }

    private HttpRequest buildRequest(URI uri, Path cachedPath) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/png,image/*;q=0.8,*/*;q=0.5")
                .GET();
        if (Files.isRegularFile(cachedPath)) {
            try {
                String lastModified = DateTimeFormatter.RFC_1123_DATE_TIME.format(
                        Files.getLastModifiedTime(cachedPath).toInstant().atZone(ZoneOffset.UTC)
                );
                builder.header("If-Modified-Since", lastModified);
            } catch (IOException exception) {
                logWarning("Impossible de lire la date du cache pour une requête conditionnelle: cache=" + cachedPath, exception);
            }
        }
        return builder.build();
    }

    private void throttle() throws TileLoadException {
        synchronized (rateLimitLock) {
            long now = System.currentTimeMillis();
            long waitMillis = nextRequestAtMillis - now;
            if (waitMillis > 0L) {
                try {
                    Thread.sleep(waitMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new TileLoadException(
                            "Temporisation des tuiles interrompue.",
                            "Interruption de la temporisation des tuiles.",
                            exception
                    );
                }
            }
            nextRequestAtMillis = System.currentTimeMillis() + requestIntervalMillis;
        }
    }

    private void validateContentType(String key, String contentType, URI uri, Path cachePath) throws TileLoadException {
        if (contentType == null || contentType.isBlank() || "absent".equals(contentType)) {
            logWarning("Type de contenu absent pour la tuile: key=" + key + ", url=" + uri + ", cache=" + cachePath, null);
            return;
        }
        String normalized = contentType.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.startsWith("image/") && !normalized.startsWith("application/octet-stream")) {
            throw new TileLoadException(
                    "Le fournisseur a renvoyé un contenu qui n'est pas une image. Voir la console.",
                    "Type de contenu invalide: key=" + key
                            + ", url=" + uri
                            + ", contentType=" + contentType
                            + ", cache=" + cachePath
            );
        }
    }

    private TileImageDiagnostics validateImageFile(Path path, String key, String source) throws TileLoadException {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new TileLoadException(
                    "Impossible de lire une tuile du cache. Voir la console.",
                    "Lecture impossible: source=" + source + ", key=" + key + ", cache=" + path,
                    exception
            );
        }
        TileImageDiagnostics diagnostics = validateImageBytes(bytes, key, path.toUri(), path);
        logImageDiagnostics(key, path, diagnostics, source);
        return diagnostics;
    }

    private TileImageDiagnostics validateImageBytes(byte[] bytes, String key, URI uri, Path cachePath) throws TileLoadException {
        if (bytes == null || bytes.length == 0) {
            throw new TileLoadException(
                    "Image de tuile vide. Voir la console.",
                    "Image vide: key=" + key + ", url=" + uri + ", cache=" + cachePath
            );
        }
        if (bytes.length > maxImageBytes) {
            throw new TileLoadException(
                    "Image de tuile trop volumineuse. Voir la console.",
                    "Image trop volumineuse: key=" + key
                            + ", url=" + uri
                            + ", cache=" + cachePath
                            + ", bytes=" + bytes.length
                            + ", maxBytes=" + maxImageBytes
            );
        }

        Image image;
        try {
            image = new Image(new ByteArrayInputStream(bytes));
        } catch (RuntimeException exception) {
            throw new TileLoadException(
                    "Image de tuile illisible. Voir la console.",
                    "Construction Image impossible: key=" + key + ", url=" + uri + ", cache=" + cachePath,
                    exception
            );
        }

        Exception imageException = image.getException();
        if (image.isError()) {
            throw new TileLoadException(
                    "JavaFX signale une erreur de décodage d'image. Voir la console.",
                    "Image errorProperty=true: key=" + key
                            + ", url=" + uri
                            + ", cache=" + cachePath
                            + ", exceptionProperty=" + imageException,
                    imageException
            );
        }

        int width = (int) Math.round(image.getWidth());
        int height = (int) Math.round(image.getHeight());
        if (width <= 0 || height <= 0) {
            throw new TileLoadException(
                    "Image de tuile sans dimensions valides. Voir la console.",
                    "Dimensions invalides: key=" + key
                            + ", url=" + uri
                            + ", cache=" + cachePath
                            + ", width=" + image.getWidth()
                            + ", height=" + image.getHeight()
                            + ", errorProperty=" + image.isError()
                            + ", exceptionProperty=" + imageException
            );
        }
        if (width > maxImageDimension || height > maxImageDimension) {
            throw new TileLoadException(
                    "Image de tuile trop grande. Voir la console.",
                    "Dimensions trop grandes: key=" + key
                            + ", url=" + uri
                            + ", cache=" + cachePath
                            + ", width=" + width
                            + ", height=" + height
                            + ", maxDimension=" + maxImageDimension
            );
        }

        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            throw new TileLoadException(
                    "Image de tuile sans pixels lisibles. Voir la console.",
                    "PixelReader indisponible: key=" + key
                            + ", url=" + uri
                            + ", cache=" + cachePath
                            + ", width=" + width
                            + ", height=" + height
                            + ", errorProperty=" + image.isError()
                            + ", exceptionProperty=" + imageException
            );
        }

        long nonTransparentPixels = 0L;
        long transparentPixels = 0L;
        Set<Integer> distinctColors = new HashSet<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = pixelReader.getArgb(x, y);
                int alpha = (argb >>> 24) & 0xFF;
                if (alpha == 0) {
                    transparentPixels++;
                } else {
                    nonTransparentPixels++;
                    distinctColors.add(argb);
                }
            }
        }

        if (nonTransparentPixels == 0L) {
            throw new TileLoadException(
                    "Image de tuile entièrement transparente. Voir la console.",
                    "Image entièrement transparente: key=" + key
                            + ", url=" + uri
                            + ", cache=" + cachePath
                            + ", width=" + width
                            + ", height=" + height
                            + ", transparentPixels=" + transparentPixels
            );
        }
        if (distinctColors.size() < minimumDistinctColors) {
            throw new TileLoadException(
                    "Image de tuile trop uniforme. Voir la console.",
                    "Image trop uniforme: key=" + key
                            + ", url=" + uri
                            + ", cache=" + cachePath
                            + ", distinctColors=" + distinctColors.size()
                            + ", minimumDistinctColors=" + minimumDistinctColors
                            + ", nonTransparentPixels=" + nonTransparentPixels
            );
        }

        return new TileImageDiagnostics(
                bytes.length,
                width,
                height,
                nonTransparentPixels,
                transparentPixels,
                distinctColors.size(),
                image.isError(),
                imageException
        );
    }

    private Image fileImage(Path path, String key, Consumer<String> statusHandler) {
        Image image = new Image(path.toUri().toString(), TILE_SIZE, TILE_SIZE, false, false, true);
        attachImageDiagnostics(image, key, path, statusHandler);
        return image;
    }

    private void attachImageDiagnostics(Image image, String key, Path path, Consumer<String> statusHandler) {
        image.errorProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                logJavaFxImageError(image, key, path, "errorProperty", statusHandler);
            }
        });
        image.exceptionProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                logJavaFxImageError(image, key, path, "exceptionProperty", statusHandler);
            }
        });
        image.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 1.0 && diagnosticsEnabled) {
                logDebug("Image JavaFX chargée: key=" + key
                        + ", cache=" + path
                        + ", width=" + image.getWidth()
                        + ", height=" + image.getHeight()
                        + ", errorProperty=" + image.isError()
                        + ", exceptionProperty=" + image.getException());
            }
        });
        if (image.isError()) {
            logJavaFxImageError(image, key, path, "initialErrorProperty", statusHandler);
        }
    }

    private void logJavaFxImageError(Image image, String key, Path path, String source, Consumer<String> statusHandler) {
        Exception exception = image.getException();
        String message = "Erreur Image JavaFX: source=" + source
                + ", key=" + key
                + ", cache=" + path
                + ", width=" + image.getWidth()
                + ", height=" + image.getHeight()
                + ", progress=" + image.getProgress()
                + ", errorProperty=" + image.isError()
                + ", exceptionProperty=" + exception;
        logError(message, exception);
        Platform.runLater(() -> {
            if (statusHandler != null) {
                statusHandler.accept("Image de tuile invalide. Voir la console.");
            }
        });
    }

    private void updateRegisteredViews(String key, Path path) {
        TileState state = tileStates.get(key);
        if (state == null) {
            return;
        }
        Platform.runLater(() -> {
            Image image = fileImage(path, key, state.statusHandler);
            state.imageViews.removeIf(reference -> {
                ImageView imageView = reference.get();
                if (imageView == null || imageView.getParent() == null) {
                    return true;
                }
                imageView.setImage(image);
                return false;
            });
        });
    }

    private void reportFailure(TileWorkItem item, TileState state, TileLoadException exception) {
        logError(exception.getMessage(), exception);
        Consumer<String> statusHandler = state.statusHandler;
        if (statusHandler != null) {
            Platform.runLater(() -> statusHandler.accept(exception.userMessage()));
        }
    }

    private TileLoadException httpStatusException(String key, HttpDiagnostics diagnostics) {
        int status = diagnostics.statusCode();
        String userMessage;
        if (status == 401 || status == 403) {
            userMessage = "Accès aux tuiles refusé (HTTP " + status + "). Vérifiez le fournisseur et le User-Agent.";
        } else if (status == 404) {
            userMessage = "Tuile introuvable (HTTP 404). Vérifiez le modèle d'URL du fournisseur.";
        } else if (status == 429) {
            userMessage = "Trop de requêtes de tuiles (HTTP 429). Augmentez la temporisation ou changez de fournisseur.";
        } else if (status >= 500) {
            userMessage = "Serveur de tuiles indisponible (HTTP " + status + "). Voir la console.";
        } else {
            userMessage = "Réponse HTTP inattendue pour les tuiles (HTTP " + status + "). Voir la console.";
        }
        return new TileLoadException(userMessage,
                "Statut HTTP invalide: key=" + key
                        + ", url=" + diagnostics.uri()
                        + ", status=" + diagnostics.statusCode()
                        + ", contentType=" + diagnostics.contentType()
                        + ", responseBytes=" + diagnostics.responseBytes()
                        + ", cache=" + diagnostics.cachePath()
                        + ", cacheControl=" + diagnostics.cacheControl()
                        + ", etag=" + diagnostics.etag()
                        + ", elapsedMillis=" + diagnostics.elapsedMillis());
    }

    private boolean isStillWorthLoading(String key, TileState state) {
        return activeTileKeys.contains(key) || inactiveForMillis(state) <= inactiveTileGraceMillis;
    }

    private long inactiveForMillis(TileState state) {
        long lastVisible = state.lastVisibleAtMillis;
        if (lastVisible <= 0L) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, System.currentTimeMillis() - lastVisible);
    }

    private boolean isRetryAllowed(TileState state) {
        long lastFailure = state.lastFailureAtMillis;
        return lastFailure <= 0L || System.currentTimeMillis() - lastFailure >= failureRetryMillis;
    }

    private void deleteInvalidCacheFile(Path path, TileLoadException cause) {
        try {
            Files.deleteIfExists(path);
            logWarning("Fichier cache invalide supprimé: cache=" + path + ", cause=" + cause.getMessage(), cause);
        } catch (IOException exception) {
            logWarning("Impossible de supprimer le fichier cache invalide: cache=" + path, exception);
        }
    }

    private boolean expired(Path cachedPath) {
        try {
            Instant lastModified = Files.getLastModifiedTime(cachedPath).toInstant();
            return lastModified.plus(MINIMUM_CACHE_TTL).isBefore(Instant.now());
        } catch (IOException exception) {
            logWarning("Impossible de lire l'âge du cache: cache=" + cachedPath, exception);
            return true;
        }
    }

    private Path cachePath(int zoom, int tileX, int tileY) {
        return cacheRoot
                .resolve(Integer.toString(zoom))
                .resolve(Integer.toString(tileX))
                .resolve(tileY + ".png");
    }

    private String tileUrl(int zoom, int tileX, int tileY) {
        return provider.urlTemplate()
                .replace("{z}", Integer.toString(zoom))
                .replace("{x}", Integer.toString(tileX))
                .replace("{y}", Integer.toString(tileY));
    }

    private void inspectCacheRoot(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastCacheInspectionAtMillis < CACHE_INSPECTION_INTERVAL.toMillis()) {
            return;
        }
        lastCacheInspectionAtMillis = now;
        boolean exists = Files.exists(cacheRoot);
        boolean directory = Files.isDirectory(cacheRoot);
        boolean hasFiles = false;
        if (directory) {
            try (var files = Files.find(cacheRoot, 5, (path, attributes) -> attributes.isRegularFile()).limit(1)) {
                hasFiles = files.findAny().isPresent();
            } catch (IOException exception) {
                logWarning("Inspection du cache impossible: cache=" + cacheRoot, exception);
            }
        }
        logInfo("État du cache des tuiles: cache=" + cacheRoot
                + ", exists=" + exists
                + ", directory=" + directory
                + ", hasFiles=" + hasFiles
                + ", activeTiles=" + activeTileKeys.size());
    }

    private void cleanupDormantTileStates() {
        long now = System.currentTimeMillis();
        long retentionMillis = Math.max(inactiveTileGraceMillis * 4L, 30_000L);
        tileStates.entrySet().removeIf(entry -> {
            TileState state = entry.getValue();
            if (state.inFlight || activeTileKeys.contains(entry.getKey())) {
                return false;
            }
            return state.lastVisibleAtMillis > 0L && now - state.lastVisibleAtMillis > retentionMillis;
        });
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

    private static long configuredLong(String name, long fallback, long minimum, long maximum) {
        String configured = System.getProperty(name);
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        try {
            long value = Long.parseLong(configured.trim());
            return Math.max(minimum, Math.min(maximum, value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean configuredBoolean(String name, boolean fallback) {
        String configured = System.getProperty(name);
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(configured.trim());
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

    private void logHttpDiagnostics(String key, HttpDiagnostics diagnostics) {
        logInfo("Réponse HTTP tuile: key=" + key
                + ", url=" + diagnostics.uri()
                + ", status=" + diagnostics.statusCode()
                + ", contentType=" + diagnostics.contentType()
                + ", responseBytes=" + diagnostics.responseBytes()
                + ", cache=" + diagnostics.cachePath()
                + ", cacheControl=" + diagnostics.cacheControl()
                + ", etag=" + diagnostics.etag()
                + ", expires=" + diagnostics.expires()
                + ", elapsedMillis=" + diagnostics.elapsedMillis());
    }

    private void logImageDiagnostics(String key, Path cachePath, TileImageDiagnostics diagnostics, String source) {
        logInfo("Diagnostic image tuile: source=" + source
                + ", key=" + key
                + ", cache=" + cachePath
                + ", bytes=" + diagnostics.bytes()
                + ", width=" + diagnostics.width()
                + ", height=" + diagnostics.height()
                + ", nonTransparentPixels=" + diagnostics.nonTransparentPixels()
                + ", transparentPixels=" + diagnostics.transparentPixels()
                + ", distinctColors=" + diagnostics.distinctColors()
                + ", errorProperty=" + diagnostics.errorProperty()
                + ", exceptionProperty=" + diagnostics.exceptionProperty());
    }

    private void logDebug(String message) {
        if (diagnosticsEnabled) {
            LOGGER.log(System.Logger.Level.DEBUG, message);
        }
    }

    private void logInfo(String message) {
        if (diagnosticsEnabled) {
            LOGGER.log(System.Logger.Level.INFO, message);
        }
    }

    private void logWarning(String message, Throwable throwable) {
        if (throwable == null) {
            LOGGER.log(System.Logger.Level.WARNING, message);
        } else {
            LOGGER.log(System.Logger.Level.WARNING, message, throwable);
        }
    }

    private void logError(String message, Throwable throwable) {
        if (throwable == null) {
            LOGGER.log(System.Logger.Level.ERROR, message);
        } else {
            LOGGER.log(System.Logger.Level.ERROR, message, throwable);
        }
    }

    @Override
    public void close() {
        closed = true;
        activeTileKeys.clear();
        workQueue.clear();
        tileStates.clear();
        tileExecutor.shutdownNow();
    }

    private static final class TileState {
        private final AtomicLong version = new AtomicLong();
        private final CopyOnWriteArrayList<WeakReference<ImageView>> imageViews = new CopyOnWriteArrayList<>();
        private volatile long lastVisibleAtMillis;
        private volatile long queuedVersion;
        private volatile long lastFailureAtMillis;
        private volatile int priority = Integer.MAX_VALUE;
        private volatile boolean inFlight;
        private volatile Consumer<String> statusHandler;

        private long markVisible(int nextPriority, Consumer<String> nextStatusHandler) {
            lastVisibleAtMillis = System.currentTimeMillis();
            priority = Math.max(0, nextPriority);
            statusHandler = nextStatusHandler;
            return version.incrementAndGet();
        }

        private void addImageView(ImageView imageView) {
            imageViews.add(new WeakReference<>(imageView));
            if (imageViews.size() > 24) {
                imageViews.removeIf(reference -> {
                    ImageView existing = reference.get();
                    return existing == null || existing.getParent() == null;
                });
            }
        }
    }

    private record TileWorkItem(
            String key,
            int zoom,
            int tileX,
            int tileY,
            Path cachedPath,
            int priority,
            long version,
            long enqueuedAtMillis,
            long sequence
    ) implements Comparable<TileWorkItem> {
        @Override
        public int compareTo(TileWorkItem other) {
            int priorityComparison = Integer.compare(priority, other.priority);
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            return Long.compare(sequence, other.sequence);
        }
    }

    private record HttpDiagnostics(
            URI uri,
            Path cachePath,
            int statusCode,
            String contentType,
            long responseBytes,
            String cacheControl,
            String etag,
            String expires,
            long elapsedMillis
    ) {
        private static HttpDiagnostics from(URI uri, Path cachePath, HttpResponse<byte[]> response, long elapsedMillis) {
            HttpHeaders headers = response.headers();
            byte[] body = response.body();
            return new HttpDiagnostics(
                    uri,
                    cachePath,
                    response.statusCode(),
                    header(headers, "Content-Type"),
                    body == null ? 0L : body.length,
                    header(headers, "Cache-Control"),
                    header(headers, "ETag"),
                    header(headers, "Expires"),
                    elapsedMillis
            );
        }

        private static String header(HttpHeaders headers, String name) {
            return headers.firstValue(name).orElse("absent");
        }
    }

    private record TileImageDiagnostics(
            long bytes,
            int width,
            int height,
            long nonTransparentPixels,
            long transparentPixels,
            int distinctColors,
            boolean errorProperty,
            Exception exceptionProperty
    ) {
    }

    private static final class TileLoadException extends Exception {
        private final String userMessage;

        private TileLoadException(String userMessage, String diagnosticMessage) {
            super(diagnosticMessage);
            this.userMessage = userMessage;
        }

        private TileLoadException(String userMessage, String diagnosticMessage, Throwable cause) {
            super(diagnosticMessage, cause);
            this.userMessage = userMessage;
        }

        private String userMessage() {
            return userMessage;
        }
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
