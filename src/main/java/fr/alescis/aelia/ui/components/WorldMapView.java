package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.service.NominatimReverseGeocoder;
import fr.alescis.aelia.service.ReverseGeocodeResult;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Interactive world map used to select and add user zones from the Carte tab.
 *
 * <p>The map is rendered natively with JavaFX image tiles instead of WebView so it
 * remains stable inside the scaled dashboard shell.</p>
 */
public final class WorldMapView extends CardPane implements AutoCloseable {
    private static final double CARD_WIDTH = 1094.0;
    private static final double CARD_HEIGHT = 822.0;
    private static final double MAP_WIDTH = 760.0;
    private static final double MAP_HEIGHT = 710.0;
    private static final double TILE_SIZE = MapTileCache.TILE_SIZE;
    private static final int MIN_ZOOM = 2;
    private static final int MAX_ZOOM = 18;
    private static final double MAX_MERCATOR_LATITUDE = 85.05112878;

    private final Consumer<LocationWeather> addLocationHandler;
    private final NominatimReverseGeocoder reverseGeocoder = new NominatimReverseGeocoder();
    private final MapTileCache tileCache = new MapTileCache(MapTileProvider.selected());
    private final AtomicInteger selectionVersion = new AtomicInteger();

    private final Pane mapViewport = new Pane();
    private final Pane tileLayer = new Pane();
    private final Pane markerLayer = new Pane();
    private final Map<String, ImageView> tileViews = new LinkedHashMap<>();
    private final Label selectedName = UiText.label("Cliquez sur la carte", "map-selection-title");
    private final Label selectedCoordinates = UiText.label("Aucune coordonnée sélectionnée", "map-coordinates");
    private final Label status = UiText.label(tileCache.diagnosticsSummary(), "map-status");
    private final Button addButton = new Button("Ajouter à mes zones");

    private ReverseGeocodeResult selectedResult;
    private int zoom = 2;
    private double centerLatitude = 20.0;
    private double centerLongitude = 0.0;
    private double centerWorldX;
    private double centerWorldY;
    private double pressSceneX;
    private double pressSceneY;
    private double pressCenterWorldX;
    private double pressCenterWorldY;
    private boolean panning;

    public WorldMapView(Consumer<LocationWeather> addLocationHandler) {
        super(CARD_WIDTH, CARD_HEIGHT);
        this.addLocationHandler = Objects.requireNonNull(addLocationHandler, "addLocationHandler");
        getStyleClass().add("map-view-card");
        Point center = project(centerLatitude, centerLongitude, zoom);
        centerWorldX = center.x();
        centerWorldY = center.y();
        buildHeader();
        buildMap();
        buildSelectionPanel();
        renderMap();
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, "Carte du monde", "Sélectionne une zone depuis une carte interactive puis l'ajoute aux lieux enregistrés.");
    }

    private void buildHeader() {
        Label title = UiText.section("Carte du monde");
        title.getStyleClass().add("map-title");
        title.setLayoutX(24);
        title.setLayoutY(18);

        Label hint = UiText.label("Déplacez la carte, zoomez, puis cliquez sur un point pour le résoudre avec Nominatim et l'ajouter à la barre latérale.", "map-body");
        hint.setLayoutX(24);
        hint.setLayoutY(44);
        hint.setPrefWidth(720);

        Label attribution = UiText.label(tileCache.provider().attribution() + " · Nominatim", "map-attribution");
        attribution.setLayoutX(800);
        attribution.setLayoutY(44);
        attribution.setPrefWidth(260);
        attribution.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(title, hint, attribution);
    }

    private void buildMap() {
        Rectangle frame = new Rectangle(MAP_WIDTH + 2, MAP_HEIGHT + 2);
        frame.setLayoutX(24);
        frame.setLayoutY(82);
        frame.setArcWidth(18);
        frame.setArcHeight(18);
        frame.getStyleClass().add("map-frame");

        mapViewport.getStyleClass().add("map-native-viewport");
        mapViewport.setLayoutX(25);
        mapViewport.setLayoutY(83);
        mapViewport.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        mapViewport.setMinSize(MAP_WIDTH, MAP_HEIGHT);
        mapViewport.setMaxSize(MAP_WIDTH, MAP_HEIGHT);
        Rectangle clip = new Rectangle(MAP_WIDTH, MAP_HEIGHT);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        mapViewport.setClip(clip);

        tileLayer.setMouseTransparent(true);
        markerLayer.setMouseTransparent(true);
        mapViewport.getChildren().addAll(tileLayer, markerLayer);
        installMapInteractions();

        getChildren().addAll(frame, mapViewport);
        addZoomButton("+", 44, 110, 1);
        addZoomButton("−", 44, 150, -1);
    }

    private void addZoomButton(String text, double x, double y, int delta) {
        Button button = new Button(text);
        button.getStyleClass().add("map-zoom-button");
        button.setLayoutX(x);
        button.setLayoutY(y);
        button.setPrefSize(34, 34);
        button.setFocusTraversable(true);
        button.setOnAction(event -> changeZoom(delta));
        getChildren().add(button);
    }

    private void installMapInteractions() {
        mapViewport.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            pressSceneX = event.getSceneX();
            pressSceneY = event.getSceneY();
            pressCenterWorldX = centerWorldX;
            pressCenterWorldY = centerWorldY;
            panning = false;
            event.consume();
        });
        mapViewport.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - pressSceneX;
            double deltaY = event.getSceneY() - pressSceneY;
            if (Math.hypot(deltaX, deltaY) > 3.0) {
                panning = true;
            }
            centerWorldX = wrapWorldX(pressCenterWorldX - deltaX);
            centerWorldY = clampCenterWorldY(pressCenterWorldY - deltaY);
            GeoCoordinate center = unproject(centerWorldX, centerWorldY, zoom);
            centerLatitude = center.latitude();
            centerLongitude = center.longitude();
            renderMap();
            event.consume();
        });
        mapViewport.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (!panning) {
                GeoCoordinate coordinate = screenToCoordinate(event.getX(), event.getY());
                selectCoordinates(coordinate.latitude(), coordinate.longitude());
            }
            event.consume();
        });
        mapViewport.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (Math.abs(event.getDeltaY()) < 1.0) {
                return;
            }
            changeZoom(event.getDeltaY() > 0.0 ? 1 : -1);
            event.consume();
        });
    }

    private void buildSelectionPanel() {
        Pane panel = new Pane();
        panel.getStyleClass().add("map-panel");
        panel.setLayoutX(812);
        panel.setLayoutY(82);
        panel.setPrefSize(258, 710);

        Label panelTitle = UiText.section("Zone sélectionnée");
        panelTitle.setLayoutX(18);
        panelTitle.setLayoutY(22);

        selectedName.setLayoutX(18);
        selectedName.setLayoutY(62);
        selectedName.setPrefWidth(222);
        selectedCoordinates.setLayoutX(18);
        selectedCoordinates.setLayoutY(96);
        selectedCoordinates.setPrefWidth(222);
        status.setLayoutX(18);
        status.setLayoutY(134);
        status.setPrefWidth(222);
        status.setWrapText(true);

        addButton.getStyleClass().add("map-add-button");
        addButton.setLayoutX(18);
        addButton.setLayoutY(206);
        addButton.setPrefSize(222, 38);
        addButton.setDisable(true);
        addButton.setAccessibleText("Ajouter la zone sélectionnée aux lieux enregistrés");
        addButton.setOnAction(event -> addSelectedLocation());

        Label noteTitle = UiText.section("Notes");
        noteTitle.setLayoutX(18);
        noteTitle.setLayoutY(292);
        Label note = UiText.label("La carte conserve les tuiles visibles pendant le déplacement et ne recrée que les tuiles qui entrent dans le viewport.", "map-note");
        note.setLayoutX(18);
        note.setLayoutY(326);
        note.setPrefWidth(222);
        note.setWrapText(true);

        Label policy = UiText.label("Cache disque ≥ 7 jours, cache mémoire de session, User-Agent applicatif et diagnostics console via -Daelia.map.*.", "map-note-muted");
        policy.setLayoutX(18);
        policy.setLayoutY(438);
        policy.setPrefWidth(222);
        policy.setWrapText(true);

        panel.getChildren().addAll(panelTitle, selectedName, selectedCoordinates, status, addButton, noteTitle, note, policy);
        getChildren().add(panel);
    }

    private void renderMap() {
        centerWorldX = wrapWorldX(centerWorldX);
        centerWorldY = clampCenterWorldY(centerWorldY);
        markerLayer.getChildren().clear();

        int tileCount = tileCount();
        double topLeftX = centerWorldX - MAP_WIDTH / 2.0;
        double topLeftY = centerWorldY - MAP_HEIGHT / 2.0;
        double centerTileX = centerWorldX / TILE_SIZE;
        double centerTileY = centerWorldY / TILE_SIZE;
        int startTileX = (int) Math.floor(topLeftX / TILE_SIZE);
        int endTileX = (int) Math.floor((topLeftX + MAP_WIDTH - 1.0) / TILE_SIZE);
        int startTileY = Math.max(0, (int) Math.floor(topLeftY / TILE_SIZE));
        int endTileY = Math.min(tileCount - 1, (int) Math.floor((topLeftY + MAP_HEIGHT - 1.0) / TILE_SIZE));

        Set<String> visibleTileKeys = new LinkedHashSet<>();
        for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                int wrappedTileX = Math.floorMod(tileX, tileCount);
                visibleTileKeys.add(tileCache.key(zoom, wrappedTileX, tileY));
            }
        }
        tileCache.setActiveTiles(visibleTileKeys);
        removeInvisibleTileViews(visibleTileKeys);

        for (int tileY = startTileY; tileY <= endTileY; tileY++) {
            for (int tileX = startTileX; tileX <= endTileX; tileX++) {
                int wrappedTileX = Math.floorMod(tileX, tileCount);
                String key = tileCache.key(zoom, wrappedTileX, tileY);
                int priority = tilePriority(tileX, tileY, centerTileX, centerTileY);
                ImageView imageView = tileViews.computeIfAbsent(key, ignored -> createTileImageView());
                if (imageView.getParent() == null) {
                    tileLayer.getChildren().add(imageView);
                }
                imageView.setLayoutX(Math.round(tileX * TILE_SIZE - topLeftX));
                imageView.setLayoutY(Math.round(tileY * TILE_SIZE - topLeftY));
                tileCache.loadTile(zoom, wrappedTileX, tileY, priority, imageView, status::setText);
            }
        }
        renderMarker();
    }

    private ImageView createTileImageView() {
        ImageView imageView = new ImageView(tileCache.placeholder());
        imageView.setFitWidth(TILE_SIZE);
        imageView.setFitHeight(TILE_SIZE);
        imageView.setSmooth(false);
        imageView.setPreserveRatio(false);
        imageView.setMouseTransparent(true);
        return imageView;
    }

    private void removeInvisibleTileViews(Set<String> visibleTileKeys) {
        Iterator<Map.Entry<String, ImageView>> iterator = tileViews.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ImageView> entry = iterator.next();
            if (!visibleTileKeys.contains(entry.getKey())) {
                tileLayer.getChildren().remove(entry.getValue());
                iterator.remove();
            }
        }
    }

    private int tilePriority(int tileX, int tileY, double centerTileX, double centerTileY) {
        double tileCenterX = tileX + 0.5;
        double tileCenterY = tileY + 0.5;
        return (int) Math.round(Math.hypot(tileCenterX - centerTileX, tileCenterY - centerTileY) * 1_000.0);
    }

    private void renderMarker() {
        if (selectedResult == null) {
            return;
        }
        Point point = coordinateToScreen(selectedResult.latitude(), selectedResult.longitude());
        if (point.x() < -20.0 || point.x() > MAP_WIDTH + 20.0 || point.y() < -20.0 || point.y() > MAP_HEIGHT + 20.0) {
            return;
        }
        Circle halo = new Circle(point.x(), point.y(), 12, Palette.withOpacity(Palette.CYAN, 0.22));
        Circle marker = new Circle(point.x(), point.y(), 6, Palette.CYAN);
        marker.setStroke(Palette.TEXT);
        marker.setStrokeWidth(2.0);
        Line stem = new Line(point.x(), point.y() + 8, point.x(), point.y() + 22);
        stem.setStroke(Palette.CYAN);
        stem.setStrokeWidth(2.0);
        Label label = UiText.label(selectedResult.city(), "map-marker-label");
        label.setLayoutX(Math.min(MAP_WIDTH - 190.0, Math.max(8.0, point.x() + 12.0)));
        label.setLayoutY(Math.min(MAP_HEIGHT - 32.0, Math.max(8.0, point.y() - 8.0)));
        label.setPrefWidth(180.0);
        markerLayer.getChildren().addAll(halo, stem, marker, label);
    }

    private void changeZoom(int delta) {
        int nextZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + delta));
        if (nextZoom == zoom) {
            return;
        }
        zoom = nextZoom;
        Point center = project(centerLatitude, centerLongitude, zoom);
        centerWorldX = center.x();
        centerWorldY = clampCenterWorldY(center.y());
        status.setText("Zoom " + zoom + " · tuiles réordonnées par proximité.");
        renderMap();
    }

    private GeoCoordinate screenToCoordinate(double screenX, double screenY) {
        double worldX = wrapWorldX(centerWorldX - MAP_WIDTH / 2.0 + screenX);
        double worldY = clamp(centerWorldY - MAP_HEIGHT / 2.0 + screenY, 0.0, worldSize() - 1.0);
        return unproject(worldX, worldY, zoom);
    }

    private Point coordinateToScreen(double latitude, double longitude) {
        Point projected = project(latitude, longitude, zoom);
        double deltaX = projected.x() - centerWorldX;
        double halfWorld = worldSize() / 2.0;
        if (deltaX > halfWorld) {
            deltaX -= worldSize();
        } else if (deltaX < -halfWorld) {
            deltaX += worldSize();
        }
        return new Point(MAP_WIDTH / 2.0 + deltaX, MAP_HEIGHT / 2.0 + projected.y() - centerWorldY);
    }

    private void selectCoordinates(double latitude, double longitude) {
        int version = selectionVersion.incrementAndGet();
        ReverseGeocodeResult fallback = ReverseGeocodeResult.coordinatesOnly(latitude, longitude);
        selectedResult = fallback;
        selectedName.setText(fallback.city());
        selectedCoordinates.setText(formatCoordinates(latitude, longitude));
        status.setText("Résolution du nom de zone…");
        addButton.setDisable(false);
        renderMap();

        reverseGeocoder.reverse(latitude, longitude).whenComplete((result, error) -> Platform.runLater(() -> {
            if (version != selectionVersion.get()) {
                return;
            }
            if (error == null && result != null) {
                selectedResult = result;
                selectedName.setText(result.city());
                selectedCoordinates.setText(formatCoordinates(result.latitude(), result.longitude()));
                status.setText(result.country() + " · prêt à ajouter.");
            } else {
                selectedResult = fallback;
                selectedName.setText(fallback.city());
                selectedCoordinates.setText(formatCoordinates(latitude, longitude));
                status.setText("Nom indisponible. La zone peut être ajoutée avec ses coordonnées.");
            }
            addButton.setDisable(false);
            renderMap();
        }));
    }

    private void addSelectedLocation() {
        if (selectedResult == null) {
            return;
        }
        addLocationHandler.accept(selectedResult.toLocationWeather());
        status.setText("Zone ajoutée : " + selectedResult.city());
    }

    private String formatCoordinates(double latitude, double longitude) {
        return String.format(Locale.ROOT, "Lat %.5f · Lon %.5f", latitude, longitude);
    }

    @Override
    public void close() {
        tileViews.clear();
        tileCache.close();
        reverseGeocoder.close();
    }

    private Point project(double latitude, double longitude, int zoomValue) {
        double clampedLatitude = clamp(latitude, -MAX_MERCATOR_LATITUDE, MAX_MERCATOR_LATITUDE);
        double sinLatitude = Math.sin(Math.toRadians(clampedLatitude));
        double scale = worldSize(zoomValue);
        double x = (longitude + 180.0) / 360.0 * scale;
        double y = (0.5 - Math.log((1.0 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * Math.PI)) * scale;
        return new Point(wrap(x, scale), clamp(y, 0.0, scale - 1.0));
    }

    private GeoCoordinate unproject(double worldX, double worldY, int zoomValue) {
        double scale = worldSize(zoomValue);
        double longitude = wrap(worldX, scale) / scale * 360.0 - 180.0;
        double mercator = Math.PI - 2.0 * Math.PI * clamp(worldY, 0.0, scale - 1.0) / scale;
        double latitude = Math.toDegrees(Math.atan(Math.sinh(mercator)));
        return new GeoCoordinate(latitude, longitude);
    }

    private int tileCount() {
        return 1 << zoom;
    }

    private double worldSize() {
        return worldSize(zoom);
    }

    private double worldSize(int zoomValue) {
        return TILE_SIZE * (1 << zoomValue);
    }

    private double wrapWorldX(double value) {
        return wrap(value, worldSize());
    }

    private double clampCenterWorldY(double value) {
        double scale = worldSize();
        if (scale <= MAP_HEIGHT) {
            return scale / 2.0;
        }
        return clamp(value, MAP_HEIGHT / 2.0, scale - MAP_HEIGHT / 2.0);
    }

    private double wrap(double value, double maximum) {
        double wrapped = value % maximum;
        return wrapped < 0.0 ? wrapped + maximum : wrapped;
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record Point(double x, double y) {
    }

    private record GeoCoordinate(double latitude, double longitude) {
    }
}
