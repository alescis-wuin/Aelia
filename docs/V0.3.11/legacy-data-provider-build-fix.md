# V0.3.11 legacy data-provider build fix

This iteration restores compilation for the legacy `fr.alescis.aelia.ports` data-provider path that can remain in local checkouts when a ZIP is extracted over an existing working tree.

## Changes

- Added `WeatherDataProvider` and restored the two-argument `WeatherUpdateListener` port.
- Reintroduced `SimulatedWeatherDataProvider` as a compatibility adapter over the current dashboard simulation catalog.
- Reintroduced `SunCycleState` with a local deterministic sunrise/sunset approximation.
- Extended `ProviderDescriptor`, `DataMetric`, `DataKind`, `MetricValue` and `SubscriptionSnapshot` with compatibility constructors and helper factories.
- Kept the dashboard rendering path unchanged: `SimulatedWeatherDashboardProvider` still drives the visible JavaFX dashboard.

## Rationale

The dashboard provider and the older data-provider port are separate integration points. The dashboard UI must keep using the mockup-oriented snapshot provider, while the legacy data-provider files must still compile so stale files do not break IntelliJ or Maven builds.
