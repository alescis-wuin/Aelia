# V0.3.17 Historical fallback and runtime provider switching

## Scope

This iteration adds a partial Open-Meteo Historical Weather fallback and runtime provider controls without changing the dashboard layout.

## Runtime provider modes

The settings view now exposes three modes:

- `simulated`: local deterministic data only;
- `auto`: simulated startup data, then remote data when Open-Meteo is available;
- `openmeteo`: remote provider mode with visible red errors when Forecast or Air Quality data cannot be loaded.

Switching a mode from the settings screen closes the active provider, creates the new provider and refreshes the dashboard immediately. Manual refresh is also available from the same view.

## Historical Weather API fallback

When the Open-Meteo Forecast API returns a transient gateway or transport error, the provider calls:

```text
https://archive-api.open-meteo.com/v1/archive
```

The fallback requests a recent historical window and maps selected variables into the dashboard read model:

- temperature;
- apparent temperature;
- relative humidity;
- sea-level pressure;
- wind speed;
- wind direction;
- wind gusts;
- historical weather code;
- sunrise, sunset and daylight duration when available.

This fallback is intentionally marked as historical. It does not pretend to be a future forecast.

## Unavailable forecast-only areas

When the dashboard runs in strict `openmeteo` mode and Forecast cannot be loaded, the visible dashboard shows a red status banner. It explains that:

- forecast cards are unavailable as true forecasts;
- hourly and seven-day cards are historical fallback values when archive data is available;
- UV, air-quality and pollen cards may remain simulated or unavailable because they depend on Forecast or Air Quality endpoints.

In `auto` mode the application keeps the fallback softer so the user can keep the dashboard open without a blocking error.

## New configuration

```text
-Daelia.openmeteo.archiveFallbackLagDays=2
-Daelia.openmeteo.archiveEndpoint=https://archive-api.open-meteo.com/v1/archive
```

`archiveFallbackLagDays` defaults to `2` because reanalysis data is not always available for the current day.

## Implementation notes

- `DashboardSnapshot` now includes `DashboardDataStatus` while preserving the previous constructor.
- `OpenMeteoArchiveClient` isolates Historical Weather API calls.
- `OpenMeteoArchiveFallbackMapper` converts archive payloads into the subset of Forecast payload shape already supported by `OpenMeteoMapper`.
- `AeliaApplication` owns runtime mode switching and refresh scheduling.
- `AeliaDashboardView` renders settings controls and a compact red status banner for strict remote errors.
