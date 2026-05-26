# V0.3.21 compile fix

## Context

V0.3.20 introduced dotenv settings and kept the multi-provider fallback chain, but the overlay ZIP missed files introduced by earlier provider iterations.

## Fixed issues

- Restored `WeatherApiComForecastClient`.
- Restored `WeatherApiComRequestFactory`.
- Restored `WeatherApiComFallbackMapper`.
- Restored `MetNorwayForecastClient`.
- Restored `MetNorwayForecastFallbackMapper`.
- Restored the `DashboardDataStatus.secondaryForecastFallback(...)` factory used by keyed fallback providers.
- Included the newer `OpenMeteoException` overloads used by the resilient HTTP client.
- Included `OpenMeteoRequestFactory` to guarantee the MET Norway URI method is present.

## Notes

This patch does not alter dashboard layout, rendering, CSS, API key names or dotenv behavior. It only restores the missing source files and related status method required by the already-referenced fallback chain.

## Verification

A partial `javac --release 21` compilation was executed on the following packages:

- `fr.alescis.aelia.model`
- `fr.alescis.aelia.provider`
- `fr.alescis.aelia.ports`
- `fr.alescis.aelia.service`

Full Maven validation still needs to be run locally because JavaFX and Maven are not installed in the execution environment used to build this ZIP.
