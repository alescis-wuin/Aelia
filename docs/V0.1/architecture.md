# Architecture V0.1

## Goal

The first version provides a minimal, executable JavaFX MVC application that can later connect to one or more remote weather APIs without changing UI code.

## Layers

- `model`: immutable domain objects such as metrics, provider descriptors, API limits and metric values.
- `ports`: the provider contract used by the service layer. Remote API adapters must implement this package.
- `provider.simulation`: local data provider used for the first functional build.
- `service`: small application service that validates requests and hides provider details from the controller.
- `ui`: JavaFX view classes and table/list view models.
- `controller`: event wiring between the view and the service.

## Provider contract

`WeatherDataProvider` exposes four capabilities:

1. List supported metrics.
2. List provider limits.
3. Return the current value of a metric.
4. Create and cancel periodic subscriptions.

The UI does not know whether values come from a simulator, Open-Meteo, Météo-France, OpenWeatherMap or another provider.

## Design choices

The code keeps model objects immutable, isolates JavaFX-specific classes in the UI/controller layer and uses a `ScheduledExecutorService` only inside the provider. This makes a remote adapter easy to add later while keeping threading decisions localized.
