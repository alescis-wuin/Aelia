# V0.4 map view integration

## Implemented

- Added a `WorldMapView` component for the `Carte` tab.
- Rendered the map with native JavaFX `ImageView` raster tiles instead of `WebView` and Leaflet.
- Added OpenStreetMap-compatible tile providers through `MapTileProvider`.
- Added tile loading through `MapTileCache` with local disk cache, throttling, viewport-aware scheduling and priority by distance to the viewport center.
- Added delayed cancellation: tiles that were visible recently can still finish loading during short pan or zoom bursts.
- Added detailed HTTP diagnostics: URL, status code, content type, response size, cache path, cache headers and elapsed time.
- Added image diagnostics: file size, image dimensions, transparent pixel count, distinct color count, JavaFX `errorProperty` and JavaFX `exceptionProperty`.
- Added validation for empty, oversized, unreadable, fully transparent or overly uniform tile images.
- Added cache inspection logs when the map is rendered: cache path existence, directory state and whether cached files are present.
- Added clearer UI status messages for HTTP, network, cache and image validation failures.
- Kept Nominatim reverse geocoding only for user-triggered map clicks, with coordinate fallback when reverse geocoding fails.
- Added selected map zones to the dashboard without changing the simulated weather provider.

## Runtime configuration

The map is configured with Java system properties:

- `aelia.map.diagnostics`
- `aelia.map.cache.dir`
- `aelia.map.tileProvider`
- `aelia.map.tileRequestIntervalMillis`
- `aelia.map.inactiveTileGraceMillis`
- `aelia.map.failureRetryMillis`
- `aelia.map.maxImageBytes`
- `aelia.map.maxImageDimension`
- `aelia.map.minimumDistinctColors`
- `aelia.map.userAgent`
- `aelia.map.tileUrl`
- `aelia.map.tileProviderName`
- `aelia.map.tileAttribution`

Built-in tile providers: `osm`, `osm-france`, `osm-france-hot`, `opentopomap`.

## Notes

- The project targets Java 26 through Maven.
- The current map implementation does not use `javafx-web`, `WebView`, Leaflet or CDN assets.
- The map intentionally does not prefetch or offline-cache areas.
- Offline maps must not be implemented against the public OpenStreetMap tile servers.

## Deferred

- Persisting saved zones to disk.
- Fetching real weather for added coordinates.
- Provider-level geocoding abstraction.
- A user-facing diagnostics panel for map errors.
