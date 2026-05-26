# V0.3.10 compatibility build fix

## Goal

This iteration corrects the API-readiness ZIP so it can be compiled by the IDE without breaking the current visual dashboard contract.

## Changes

- `CurrentWeather` is now a final immutable class instead of a record.
- The class preserves the original string accessors: `dateLabel()`, `sunrise()`, `sunset()`, `daylightDuration()` and `currentSolarTime()`.
- Typed values remain available through `date()`, `zoneId()`, `sunriseTime()`, `sunsetTime()`, `currentSolarLocalTime()` and `daylightDurationValue()`.
- `GaugeMath` now returns the project-owned `GaugePoint` record instead of JavaFX `Point2D`.
- `PressureCard` and `SunPathCard` use `GaugePoint` for computed geometry.
- JavaFX constructor warnings related to fixed-size view initialization are suppressed where the calls are intentional and safe.

## Rationale

The previous iteration correctly moved weather time data toward typed Java values, but it was too strict for a project that still contains mockup-oriented and compatibility-oriented code. Restoring the old string getters avoids regressions while keeping typed access for future remote API providers.

The gauge math helper no longer depends on JavaFX geometry classes. This keeps pure numeric tests lighter and makes the helper easier to reuse outside UI nodes.

## Validation

The complete main and test source tree was compiled with `javac --release 21 -Xlint:all` using local JavaFX and JUnit compatibility stubs because Maven dependencies are not available in the execution container. This validates project-level Java type consistency, including the current-weather API, gauge geometry helpers and provider code.

The final validation must still be run locally with Maven to use the real JavaFX, AtlantaFX and JUnit artifacts.
