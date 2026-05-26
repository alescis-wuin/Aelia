# V0.3.15 Open-Meteo timeout and startup robustness fix

## Problem observed

The V0.3.14 runtime reached the Open-Meteo provider correctly, but strict `openmeteo` mode executed the first remote snapshot synchronously during the JavaFX startup sequence. A network connect timeout therefore aborted the JavaFX application before the scene could remain visible.

The console also displayed Open-Meteo URLs with `%252F` and `%252C`. The actual request URI was encoded once, but the diagnostic redaction helper rebuilt the URI from an already-escaped raw query and double-escaped percent signs while printing the log line.

## Changes

- Strict `openmeteo` mode no longer crashes the application if the first remote call fails.
- On first-call failure, Aelia starts with the simulated snapshot and keeps retrying the Open-Meteo snapshot asynchronously.
- `auto` mode now uses a simulated startup snapshot plus a direct asynchronous Open-Meteo refresh; it no longer silently replaces the UI with another simulated snapshot when the remote refresh fails.
- Open-Meteo connect timeout default changed from 3 seconds to 10 seconds.
- Open-Meteo request timeout default changed from 6 seconds to 20 seconds.
- Runtime timeout settings can be configured through Maven properties or environment variables.
- The JavaFX Maven plugin now forwards timeout settings and `java.net.preferIPv4Stack` to the forked JavaFX JVM.
- Open-Meteo diagnostic URL printing no longer double-encodes percent signs.
- HTTP error diagnostics include a short response body excerpt when Open-Meteo returns a non-2xx response.

## Useful runtime switches

```bash
mvn clean \
  -Daelia.weather.provider=openmeteo \
  -Daelia.openmeteo.connectTimeoutSeconds=20 \
  -Daelia.openmeteo.requestTimeoutSeconds=40 \
  -Djava.net.preferIPv4Stack=true \
  javafx:run
```

Equivalent environment variables:

```bash
export AELIA_WEATHER_PROVIDER=openmeteo
export AELIA_OPENMETEO_CONNECT_TIMEOUT_SECONDS=20
export AELIA_OPENMETEO_REQUEST_TIMEOUT_SECONDS=40
```

## Network triage

When Open-Meteo still times out, verify connectivity outside JavaFX:

```bash
curl -v --connect-timeout 10 'https://api.open-meteo.com/v1/forecast?latitude=48.8566&longitude=2.3522&timezone=Europe%2FParis&current=temperature_2m&forecast_days=1'
```

If curl succeeds but Java still times out, retry with:

```bash
mvn clean -Djava.net.preferIPv4Stack=true -Daelia.weather.provider=openmeteo javafx:run
```
