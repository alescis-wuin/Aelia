# Provider contract

The UI is backed by `WeatherDashboardProvider`.

## Supported operations

- Read a dashboard snapshot.
- List supported metrics.
- List API/provider limits.
- Request the current value of a metric.
- Subscribe to periodic metric readings.
- Unsubscribe from a stream.

## Simulation

`SimulatedWeatherDashboardProvider` uses deterministic Paris values to reproduce the visual mockup. On-demand metric readings include small bounded variations so future technical panels can display evolving data without needing a remote API.

## Future remote adapters

A remote adapter should implement the same interface and convert API-specific payloads into the model records used by the UI. The UI must not depend on remote API response schemas.
