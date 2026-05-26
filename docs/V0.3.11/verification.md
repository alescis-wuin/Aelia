# V0.3.11 verification

## Local validation

The non-UI application, model, provider, service and legacy ports were compiled with:

```bash
javac --release 21 -Xlint:all
```

A smoke test instantiated `SimulatedWeatherDataProvider`, read a current metric, opened a short humidity subscription and unsubscribed it.

## Scope

The visible dashboard provider remains `SimulatedWeatherDashboardProvider`. The restored `SimulatedWeatherDataProvider` exists to keep the older generic data-provider path buildable while the application is still using the mockup-oriented dashboard snapshot model.
