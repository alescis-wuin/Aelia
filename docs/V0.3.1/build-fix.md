# Build fix V0.3.1

This maintenance iteration addresses a likely JavaFX compilation issue found after importing the V0.3 dashboard into an IDE build.

## Changes

- `ScaledDashboardShell` now stores the result of `Bindings.min(...)` as `NumberBinding`, which matches the JavaFX API more safely than `DoubleBinding`.
- `DailyForecastCard` uses `AccessibleRole.LIST_ITEM` for custom forecast rows instead of a table-row role, improving compatibility across JavaFX versions.
- Maven project version updated to `0.3.1-SNAPSHOT`.

## Validation notes

The full JavaFX Maven build must be run locally with Maven and JavaFX dependencies available:

```bash
mvn clean verify
mvn javafx:run
```
