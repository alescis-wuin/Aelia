# V0.3.9 remote provider readiness plan

## Completed in this iteration

1. Keep the current simulated provider and rendering behavior.
2. Replace display-formatted current-weather temporal fields with typed values.
3. Move date, time and daylight formatting into `UiFormatters`.
4. Add `GaugeMath` as the single source for gauge normalization and arc positions.
5. Make wind, pressure, air-quality and sun cards data-driven.
6. Bind `CardPane` clipping to current pane dimensions.
7. Add regression-oriented tests for typed temporal values, provider delegation, subscriptions and gauge math.

## Next implementation phase

### Provider package

Add a dedicated package:

```text
fr.alescis.aelia.provider.openmeteo
```

Suggested classes:

```text
OpenMeteoWeatherDashboardProvider
OpenMeteoForecastClient
OpenMeteoAirQualityClient
OpenMeteoGeocodingClient
OpenMeteoRequestFactory
OpenMeteoMapper
OpenMeteoException
```

### Remote execution model

Use `java.net.http.HttpClient` with:

- explicit connect timeout;
- per-request timeout;
- no network work on the JavaFX Application Thread;
- one bounded executor for remote calls;
- cache entries with per-endpoint TTL;
- retry only for transient transport failures;
- clear handling of HTTP 429 and provider quota exhaustion.

### Cache policy

Recommended starting TTLs:

- geocoding: one day or longer;
- current and hourly forecast: 10 to 20 minutes;
- daily forecast: 30 to 60 minutes;
- air quality: 30 to 60 minutes;
- pollen: 3 to 6 hours;
- astronomy: 12 to 24 hours.

### UI states

Before enabling remote providers, cards should support:

- loaded value;
- loading value;
- stale cached value;
- unavailable value;
- partial provider error.

Those states should be modeled once and reused by all cards.
