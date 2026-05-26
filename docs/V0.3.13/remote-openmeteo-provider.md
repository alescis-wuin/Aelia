# V0.3.13 remote Open-Meteo provider

## Scope

This iteration adds a production-oriented Open-Meteo remote provider while preserving the current JavaFX rendering path.

The application still renders the existing simulated dashboard immediately, then refreshes the same dashboard layout asynchronously with remote Open-Meteo data when the provider mode is `auto` or `openmeteo`. This prevents network latency from blocking the JavaFX Application Thread and keeps the existing mockup dimensions and visual hierarchy unchanged.

## Added packages

```text
fr.alescis.aelia.provider.openmeteo
fr.alescis.aelia.provider
```

## Provider modes

Provider mode can be selected with either a system property or an environment variable:

```bash
-Daelia.weather.provider=auto
-Daelia.weather.provider=simulated
-Daelia.weather.provider=openmeteo
```

```bash
AELIA_WEATHER_PROVIDER=auto
AELIA_WEATHER_PROVIDER=simulated
AELIA_WEATHER_PROVIDER=openmeteo
```

The default mode is `auto`.

| Mode | Behavior |
| --- | --- |
| `simulated` | Uses the deterministic local provider only. |
| `openmeteo` | Uses Open-Meteo directly. Startup still paints the local snapshot before the first remote refresh. |
| `auto` | Uses Open-Meteo with an automatic simulated-provider fallback. |

## Runtime location override

The selected location can be overridden without changing source code:

```bash
-Daelia.openmeteo.latitude=49.4431 \
-Daelia.openmeteo.longitude=1.0993 \
-Daelia.openmeteo.city=Rouen \
-Daelia.openmeteo.country=France \
-Daelia.openmeteo.timezone=Europe/Paris
```

Equivalent environment variables are also supported:

```bash
AELIA_OPENMETEO_LATITUDE=49.4431
AELIA_OPENMETEO_LONGITUDE=1.0993
AELIA_OPENMETEO_CITY=Rouen
AELIA_OPENMETEO_COUNTRY=France
AELIA_OPENMETEO_TIMEZONE=Europe/Paris
```

## Remote data coverage

The Open-Meteo implementation maps these remote values into the current dashboard model:

- current temperature;
- apparent temperature;
- daily minimum and maximum temperature;
- humidity;
- weather code mapped to the existing `WeatherCondition` enum;
- sunrise and sunset;
- hourly forecast temperatures and weather codes;
- daily forecast temperatures, weather codes, rain probability, wind maximum and UV maximum;
- wind speed, gusts and direction;
- sea-level pressure and pressure trend;
- UV index and advice text;
- European AQI;
- PM2.5, PM10 and nitrogen dioxide;
- grass, birch, olive and ragweed pollen risk levels.

## Cache and network policy

`OpenMeteoHttpClient` uses `java.net.http.HttpClient`, explicit connect/request timeouts, a per-URI in-memory TTL cache and defensive HTTP error handling.

Initial TTLs:

| Endpoint | TTL |
| --- | ---: |
| forecast | 15 min |
| air quality / pollen | 45 min |
| geocoding | 1 day |

HTTP `429` is mapped to a dedicated `OpenMeteoException`, then handled by the failover provider in `auto` mode.

## API attribution and limitations

Open-Meteo free/open-access usage is intended for non-commercial use and has published request limits. Commercial usage requires the customer endpoint and API key. The implementation supports an optional API key through:

```bash
-Daelia.openmeteo.apiKey=...
AELIA_OPENMETEO_API_KEY=...
```

The Open-Meteo air-quality endpoint exposes pollen data only in Europe during pollen season. Outside those conditions, missing pollen values are mapped to the `NONE` risk level to keep the UI stable.

## Design choices

- No external JSON dependency was introduced; a small JSON parser is scoped to the Open-Meteo provider package.
- The existing `WeatherDashboardProvider` interface remains the dashboard read-model port.
- The newer `WeatherDataProvider` interface is supported through `OpenMeteoWeatherDataProvider` without mixing incompatible return types into the dashboard provider.
- `FailoverWeatherDashboardProvider` provides resilience without duplicating UI logic.
- The JavaFX entry point does not perform remote work on the JavaFX Application Thread.
