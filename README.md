# Aelia

Aelia is a Java 26, JavaFX, Maven and AtlantaFX desktop weather dashboard prototype.

This branch combines the native JavaFX **Carte** tab with restored weather provider modes: local simulation, automatic remote refresh and strict API-oriented execution. The primary remote provider is Open-Meteo, with optional keyed fallbacks for WeatherAPI.com, Visual Crossing, OpenWeather, Weatherbit and Pirate Weather.

## Requirements

- Java 26.
- Maven 3.9+.
- Internet access for map tiles, reverse geocoding and remote weather APIs.

## Run

```bash
mvn javafx:run
```

## Provider modes

```bash
mvn javafx:run -Daelia.weather.provider=auto
mvn javafx:run -Daelia.weather.provider=api
mvn javafx:run -Daelia.weather.provider=simulated
```

- `auto`: starts with the simulated snapshot, then refreshes with remote providers and falls back to simulation if every remote source fails.
- `api`: loads remote providers first and displays a visible provider status if the remote chain fails.
- `simulated`: uses only deterministic local data.

The `Réglages` view exposes buttons for `auto`, `api`, `simulated` and remote refresh.

## Remote weather configuration

Open-Meteo is keyless for the public forecast and air-quality endpoints. The keyed fallbacks are used only when their key is present and their `AELIA_*_ENABLED` flag is enabled.

```bash
AELIA_WEATHER_PROVIDER=auto
AELIA_OPENMETEO_LATITUDE=49.4431
AELIA_OPENMETEO_LONGITUDE=1.0993
AELIA_OPENMETEO_CITY=Rouen
AELIA_OPENMETEO_COUNTRY=France
AELIA_OPENMETEO_TIMEZONE=Europe/Paris
AELIA_WEATHERAPI_API_KEY=
AELIA_VISUALCROSSING_API_KEY=
AELIA_OPENWEATHER_API_KEY=
AELIA_WEATHERBIT_API_KEY=
AELIA_PIRATEWEATHER_API_KEY=
```

Java system properties with the same logical names are also supported, for example `-Daelia.openmeteo.latitude=49.4431`.

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

The map intentionally does not prefetch/offline-cache areas. It keeps visible tile nodes during panning, repositions already visible tiles, schedules newly visible tiles first, keeps recently visible tiles briefly eligible for loading, and stores valid tiles in local disk and in-memory session caches.

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
-Daelia.map.rejectUniformTiles=false
-Daelia.map.userAgent="Aelia/0.4.1 (+https://github.com/alescis-wuin/Aelia; contact: your-contact)"
```

Uniform-but-valid tiles, such as plain ocean tiles, are accepted by default and logged as warnings. Set `aelia.map.rejectUniformTiles=true` only when intentionally testing strict validation.

Built-in tile providers: `osm`, `osm-france`, `osm-france-hot`, `opentopomap`.

A custom tile URL can be provided with:

```bash
-Daelia.map.tileUrl="https://example.org/tiles/{z}/{x}/{y}.png"
-Daelia.map.tileProviderName="Custom tiles"
-Daelia.map.tileAttribution="© Custom provider"
```

## Attribution

The map view displays tile attribution on the map. Reverse geocoding uses Nominatim only after user-triggered clicks and identifies the application with a stable User-Agent.
