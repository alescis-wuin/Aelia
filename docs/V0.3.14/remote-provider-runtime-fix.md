# V0.3.14 remote provider runtime fix

## Problem

The Open-Meteo integration compiled, but the visible dashboard could remain identical to the deterministic simulation. Two causes made this hard to diagnose:

1. The JavaFX application always started from `WeatherProviderFactory.initialSnapshot()`, which returned a simulated snapshot unconditionally.
2. Asynchronous remote-refresh errors were swallowed, so an Open-Meteo HTTP, JSON, network or mapping failure left the simulation visible without a console explanation.

Maven `-D` settings passed to `mvn javafx:run` also needed to be forwarded explicitly as Java runtime options. Without that, application-level calls to `System.getProperty(...)` could miss command-line provider and location settings depending on the JavaFX Maven plugin launch path.

## Changes

- `WeatherProviderFactory.initialSnapshot(WeatherDashboardProvider)` now loads the real provider synchronously in strict `openmeteo` mode.
- `auto` mode keeps the simulation as the immediate startup snapshot and refreshes remotely in the background.
- Remote refresh failures are printed with stack traces instead of being ignored.
- The JavaFX Maven plugin forwards Aelia provider and Open-Meteo properties to the launched Java process.
- Provider diagnostics print the selected mode, requested Open-Meteo URLs, HTTP statuses and successful mapping summary.
- Open-Meteo air-quality failures no longer block real weather values; the dashboard maps weather values and keeps unavailable air/pollen values safe.
- Inactive sidebar locations no longer trigger extra startup HTTP requests; this keeps the first remote refresh focused on the selected location.
- The cached snapshot method is now used by `currentValue` and the generic data-provider adapter.
- The unused dashboard-level geocoding method was removed. `OpenMeteoGeocodingClient` remains as a dedicated client for a future city-search UI.

## Runtime modes

### Simulation

```bash
mvn -Daelia.weather.provider=simulated javafx:run
```

### Strict Open-Meteo

```bash
mvn -Daelia.weather.provider=openmeteo \
  -Daelia.openmeteo.latitude=49.4431 \
  -Daelia.openmeteo.longitude=1.0993 \
  -Daelia.openmeteo.city=Rouen \
  -Daelia.openmeteo.country=France \
  -Daelia.openmeteo.timezone=Europe/Paris \
  javafx:run
```

Strict mode should show real remote values at first render. If the remote request fails, the application fails loudly with the underlying exception instead of silently keeping the simulation.

### Auto failover

```bash
mvn -Daelia.weather.provider=auto javafx:run
```

Auto mode opens immediately on the simulated snapshot, then replaces it with Open-Meteo values if the remote refresh succeeds. If remote refresh fails, the console contains a warning and the simulated snapshot remains visible.

## Expected console markers

A working remote run should contain messages similar to:

```text
[Aelia] ... INFO Selected weather provider mode: openmeteo.
[Aelia] ... INFO Loading initial Open-Meteo snapshot synchronously.
[Aelia] ... INFO Loading Open-Meteo snapshot for Rouen (49.4431, 1.0993).
[Aelia] ... INFO Open-Meteo GET https://api.open-meteo.com/v1/forecast?...
[Aelia] ... INFO Open-Meteo HTTP 200 for https://api.open-meteo.com/v1/forecast?...
[Aelia] ... INFO Open-Meteo snapshot mapped for Rouen with 9 hourly points and 7 daily points.
```

If those lines do not appear, the JavaFX runtime did not receive the provider configuration or the patched Maven plugin configuration is not in use.
