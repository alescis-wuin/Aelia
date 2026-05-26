# V0.4 map view integration

## Implemented

- Added JavaFX WebView support through `javafx-web`.
- Added a `WorldMapView` component for the `Carte` tab.
- Embedded Leaflet in a local HTML document loaded by WebView.
- Added OpenStreetMap tile layer attribution.
- Added click-to-select coordinates, marker placement and popup feedback.
- Added a JavaScript-to-Java bridge for selected latitude/longitude.
- Added a small Nominatim reverse-geocoder with explicit User-Agent, timeout and a one-request-per-second guard.
- Added coordinate fallback when reverse geocoding fails.
- Added dynamic sidebar insertion through `SidebarView.addLocation`.
- Added selected map zones to the dashboard without changing the simulated weather provider.

## Deferred

- Persisting saved zones to disk.
- Fetching real weather for added coordinates.
- Provider-level geocoding abstraction.
- Configurable tile provider URL.
- Offline maps, which must not be implemented against `tile.openstreetmap.org`.
