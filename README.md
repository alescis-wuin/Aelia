# Aelia

Aelia is a JavaFX, Maven and AtlantaFX desktop weather dashboard prototype.

This package adds a functional **Carte** tab backed by JavaFX `WebView`, Leaflet, OpenStreetMap raster tiles and Nominatim reverse geocoding. The dashboard data remains simulated; selected map locations are added to the local sidebar list and can be selected immediately.

## Requirements

- Java 21 or newer. Java 26 is supported as a runtime.
- Maven 3.9+
- Internet access for the map tiles, Leaflet CDN assets and reverse geocoding.

## Run

```bash
mvn javafx:run
```

## Test

```bash
mvn test
```

## Map usage

1. Open the **CARTE** tab.
2. Click a location on the world map.
3. Wait for the zone name to resolve, or keep the coordinate-based fallback.
4. Click **Ajouter à mes zones**.
5. The zone is appended to the sidebar and selected.

The map intentionally does not prefetch/offline-cache areas. It only displays tiles for the current human-driven viewport.

## Attribution

The map view displays OpenStreetMap attribution on the map. Reverse geocoding uses Nominatim only after user-triggered clicks and identifies the application with a stable User-Agent.
