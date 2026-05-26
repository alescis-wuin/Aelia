# V0.3.16 Open-Meteo 502 resilience

## Context

The V0.3.15 runtime confirmed that the Open-Meteo provider is selected and called correctly, but the public endpoint can return HTTP 502 Bad Gateway for the complete forecast request. When that happens, the dashboard remains usable through the simulated startup snapshot, but real values cannot replace it until a successful remote response is received.

## Changes

- Added explicit transient-failure metadata to `OpenMeteoException`.
- Marked HTTP 408 and HTTP 5xx responses as transient provider failures.
- Added a reduced Open-Meteo forecast request that omits secondary current variables not needed by the mapper.
- Added automatic reduced-request retry when the complete forecast request fails with a transient HTTP or transport error.
- Replaced the single asynchronous refresh attempt with a scheduled retry loop.
- Added exponential-style retry delays after failures: 5s, 15s, 30s, 1min, 2min, then 5min.
- Added a regular 15-minute refresh after a successful remote snapshot.

## Expected behavior

- The UI never crashes because of an Open-Meteo 502 response.
- The current visible snapshot remains unchanged while Open-Meteo is unavailable.
- Aelia keeps retrying in the background and replaces the visible dashboard once a remote snapshot is successfully mapped.
- If the complete forecast request triggers a provider gateway failure but the reduced request succeeds, the dashboard receives real values without waiting for the next scheduled retry.

## Manual checks

```bash
mvn clean \
  -Daelia.weather.provider=openmeteo \
  -Daelia.openmeteo.connectTimeoutSeconds=20 \
  -Daelia.openmeteo.requestTimeoutSeconds=40 \
  -Djava.net.preferIPv4Stack=true \
  javafx:run
```

The console should show both the complete request and, when needed, a reduced-request retry before scheduling the next background retry.
