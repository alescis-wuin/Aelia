package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.service.NominatimReverseGeocoder;
import fr.alescis.aelia.service.ReverseGeocodeResult;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Interactive world map used to select and add user zones from the Carte tab.
 */
public final class WorldMapView extends CardPane implements AutoCloseable {
    private static final double CARD_WIDTH = 1094.0;
    private static final double CARD_HEIGHT = 822.0;
    private static final double MAP_WIDTH = 760.0;
    private static final double MAP_HEIGHT = 710.0;
    private static final String TILE_URL_TEMPLATE = System.getProperty("aelia.map.tileUrl", "https://tile.openstreetmap.org/{z}/{x}/{y}.png");

    private final Consumer<LocationWeather> addLocationHandler;
    private final NominatimReverseGeocoder reverseGeocoder = new NominatimReverseGeocoder();
    private final AtomicInteger selectionVersion = new AtomicInteger();
    private final JavaBridge javaBridge = new JavaBridge();

    private final Label selectedName = UiText.label("Cliquez sur la carte", "map-selection-title");
    private final Label selectedCoordinates = UiText.label("Aucune coordonnée sélectionnée", "map-coordinates");
    private final Label status = UiText.label("La carte utilise Leaflet et OpenStreetMap.", "map-status");
    private final Button addButton = new Button("Ajouter à mes zones");

    private ReverseGeocodeResult selectedResult;

    public WorldMapView(Consumer<LocationWeather> addLocationHandler) {
        super(CARD_WIDTH, CARD_HEIGHT);
        this.addLocationHandler = Objects.requireNonNull(addLocationHandler, "addLocationHandler");
        getStyleClass().add("map-view-card");
        buildHeader();
        buildMap();
        buildSelectionPanel();
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, "Carte du monde", "Sélectionne une zone depuis une carte interactive puis l'ajoute aux lieux enregistrés.");
    }

    private void buildHeader() {
        Label title = UiText.section("Carte du monde");
        title.getStyleClass().add("map-title");
        title.setLayoutX(24);
        title.setLayoutY(18);

        Label hint = UiText.label("Cliquez sur un point de la carte pour le résoudre avec Nominatim, puis ajoutez-le à la barre latérale.", "map-body");
        hint.setLayoutX(24);
        hint.setLayoutY(44);
        hint.setPrefWidth(720);

        Label attribution = UiText.label("© OpenStreetMap contributors · Leaflet · Nominatim", "map-attribution");
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

        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setLayoutX(25);
        webView.setLayoutY(83);
        webView.setPrefSize(MAP_WIDTH, MAP_HEIGHT);
        webView.setMinSize(MAP_WIDTH, MAP_HEIGHT);
        webView.setMaxSize(MAP_WIDTH, MAP_HEIGHT);
        WebEngine engine = webView.getEngine();
        engine.setUserAgent(NominatimReverseGeocoder.USER_AGENT);
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("aeliaBridge", javaBridge);
                engine.executeScript("if (window.aeliaBridgeReady) { window.aeliaBridgeReady(); }");
                status.setText("Carte chargée. Cliquez sur une zone.");
            } else if (newState == Worker.State.FAILED) {
                status.setText("Carte indisponible. Vérifiez la connexion internet.");
            }
        });
        engine.loadContent(mapHtml(), "text/html");

        getChildren().addAll(frame, webView);
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
        Label note = UiText.label("Les données météo restent simulées dans cette itération. Les coordonnées seront réutilisables par un futur provider météo distant.", "map-note");
        note.setLayoutX(18);
        note.setLayoutY(326);
        note.setPrefWidth(222);
        note.setWrapText(true);

        Label policy = UiText.label("Aucun préchargement massif ni mode hors-ligne n'est activé afin de respecter les règles d'usage des tuiles publiques.", "map-note-muted");
        policy.setLayoutX(18);
        policy.setLayoutY(438);
        policy.setPrefWidth(222);
        policy.setWrapText(true);

        panel.getChildren().addAll(panelTitle, selectedName, selectedCoordinates, status, addButton, noteTitle, note, policy);
        getChildren().add(panel);
    }

    private void selectCoordinates(double latitude, double longitude) {
        int version = selectionVersion.incrementAndGet();
        ReverseGeocodeResult fallback = ReverseGeocodeResult.coordinatesOnly(latitude, longitude);
        selectedResult = fallback;
        selectedName.setText(fallback.city());
        selectedCoordinates.setText(formatCoordinates(latitude, longitude));
        status.setText("Résolution du nom de zone…");
        addButton.setDisable(false);

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
        reverseGeocoder.close();
    }

    /**
     * Public JavaScript bridge. Must remain public for JavaFX WebEngine reflection.
     */
    public final class JavaBridge {
        public void onMapClicked(double latitude, double longitude) {
            Platform.runLater(() -> selectCoordinates(latitude, longitude));
        }
    }

    private String mapHtml() {
        return """
                <!doctype html>
                <html lang=\"fr\">
                <head>
                  <meta charset=\"utf-8\">
                  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
                  <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\">
                  <style>
                    html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; background: #0B1220; }
                    body { overflow: hidden; font-family: Luciole, Segoe UI, Arial, sans-serif; }
                    #map { border-radius: 16px; }
                    .leaflet-container { background: #0B1220; color: #D9E2F2; }
                    .leaflet-control-attribution { background: rgba(7, 11, 20, 0.82) !important; color: #9AA7BA !important; }
                    .leaflet-control-attribution a { color: #00E5FF !important; }
                    .leaflet-control-zoom a { background: #101B31 !important; color: #FFFFFF !important; border-color: #1E2E48 !important; }
                    .aelia-popup { color: #0B1220; font-weight: 700; }
                    .map-error { color: #00E5FF; padding: 24px; font-weight: 700; }
                  </style>
                </head>
                <body>
                  <div id=\"map\"><div class=\"map-error\">Chargement de la carte…</div></div>
                  <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>
                  <script>
                    (function () {
                      let map;
                      let marker;
                      let pendingPoint = null;

                      function notifyJava(lat, lng) {
                        if (window.aeliaBridge) {
                          window.aeliaBridge.onMapClicked(lat, lng);
                        } else {
                          pendingPoint = {lat: lat, lng: lng};
                        }
                      }

                      function selectPoint(lat, lng, notify) {
                        const text = lat.toFixed(5) + ', ' + lng.toFixed(5);
                        if (!marker) {
                          marker = L.marker([lat, lng]).addTo(map);
                        } else {
                          marker.setLatLng([lat, lng]);
                        }
                        marker.bindPopup('<span class=\"aelia-popup\">Zone sélectionnée<br>' + text + '</span>').openPopup();
                        if (notify) {
                          notifyJava(lat, lng);
                        }
                      }

                      function init() {
                        if (typeof L === 'undefined') {
                          document.getElementById('map').innerHTML = '<div class=\"map-error\">Leaflet est indisponible. Vérifiez la connexion internet.</div>';
                          return;
                        }
                        map = L.map('map', {
                          worldCopyJump: true,
                          zoomControl: true,
                          attributionControl: true
                        }).setView([20, 0], 2);
                        L.tileLayer('__TILE_URL__', {
                          minZoom: 2,
                          maxZoom: 18,
                          attribution: '&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors'
                        }).addTo(map);
                        map.on('click', function (event) {
                          selectPoint(event.latlng.lat, event.latlng.lng, true);
                        });
                      }

                      window.aeliaBridgeReady = function () {
                        if (pendingPoint) {
                          window.aeliaBridge.onMapClicked(pendingPoint.lat, pendingPoint.lng);
                          pendingPoint = null;
                        }
                      };

                      document.addEventListener('DOMContentLoaded', init);
                    }());
                  </script>
                </body>
                </html>
                """.replace("__TILE_URL__", escapeJavaScript(TILE_URL_TEMPLATE));
    }

    private String escapeJavaScript(String value) {
        return value.replace("\\", "\\\\").replace("\'", "\\\'");
    }
}
