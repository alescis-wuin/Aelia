# Aelia

Aelia is a Java 26, JavaFX, Maven and AtlantaFX desktop weather dashboard prototype.

This branch adds a functional **Carte** tab backed by a native JavaFX raster tile view, OpenStreetMap-compatible tile providers and Nominatim reverse geocoding. The dashboard data remains simulated; selected map locations are added to the local sidebar list and can be selected immediately.

## Requirements

- Java 26.
- Maven 3.9+.
- Internet access for map tiles and reverse geocoding.

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

The map intentionally does not prefetch/offline-cache areas. It schedules visible tiles first, keeps recently visible tiles briefly eligible for loading, and stores valid tiles in a local disk cache.

## Map diagnostics and runtime properties

The native tile loader logs HTTP and image diagnostics to the console: tile URL, HTTP status, content type, response size, cache path, cache state, JavaFX `errorProperty`, JavaFX `exceptionProperty`, image dimensions, transparent pixels and distinct color count.

Useful Java system properties:

```bash
-Daelia.map.diagnostics=true
-Daelia.map.cache.dir=/path/to/cache
-Daelia.map.tileProvider=osm
-Daelia.map.tileRequestIntervalMillis=350
-Daelia.map.inactiveTileGraceMillis=2500
-Daelia.map.failureRetryMillis=5000
-Daelia.map.maxImageBytes=5242880
-Daelia.map.maxImageDimension=1024
-Daelia.map.minimumDistinctColors=2
-Daelia.map.userAgent="Aelia/0.4.1 (+https://github.com/alescis-wuin/Aelia; contact: your-contact)"
```

Built-in tile providers: `osm`, `osm-france`, `osm-france-hot`, `opentopomap`.

A custom tile URL can be provided with:

```bash
-Daelia.map.tileUrl="https://example.org/tiles/{z}/{x}/{y}.png"
-Daelia.map.tileProviderName="Custom tiles"
-Daelia.map.tileAttribution="© Custom provider"
```

## Attribution

The map view displays tile attribution on the map. Reverse geocoding uses Nominatim only after user-triggered clicks and identifies the application with a stable User-Agent.
