# V0.3.9 API readiness implementation notes

## Scope

This iteration prepares the dashboard for future remote data providers without changing the current simulated rendering strategy.

The implementation keeps the existing high-contrast dashboard layout and simulated Paris dataset, while removing the main API-readiness weaknesses that were previously identified:

- decorative gauge progress hardcoded inside metric cards;
- hardcoded sun position and elapsed daylight arc;
- fixed pressure marker despite dynamic pressure arc;
- fixed air-quality arc;
- display-formatted temporal strings in the current-weather model;
- fixed card clip that could not follow future resized cards.

## Main changes

### Typed current-weather temporal model

`CurrentWeather` now stores date and time-zone aware temporal data as typed values:

- `LocalDate date`;
- `ZoneId zoneId`;
- `LocalTime sunriseTime`;
- `LocalTime sunsetTime`;
- `LocalTime currentSolarTime`.

The model derives daylight duration and daylight progress from these typed values. Display strings are produced by `UiFormatters`, not stored in the domain model.

### Shared gauge math

`GaugeMath` centralizes clamping, normalization and arc-position calculations. This prevents each card from duplicating geometry rules and makes the next provider integration safer.

Current uses:

- wind speed progress;
- pressure arc and pressure marker;
- air-quality arc;
- sun path elapsed arc and sun marker position.

### Data-driven gauges

The following components now derive their visual state from input data:

- `WindCard`: progress comes from speed normalized against the displayed speed range;
- `PressureCard`: progress and marker position come from pressure normalized between 980 and 1040 hPa;
- `AirQualityCard`: progress comes from the current IQA value normalized against the good-range upper bound;
- `SunPathCard`: elapsed daylight arc and sun position come from `CurrentWeather.daylightProgress()`.

The current mock values intentionally preserve the same visual proportions as before.

### Resizable card clipping

`CardPane` still keeps the fixed mockup size as preferred, minimum and maximum size. Its clip is now bound to the actual pane size, so future denser or responsive states can resize safely without stale clipping geometry.

## Non-goals

This iteration does not add a remote API provider. The application still uses `SimulatedWeatherDashboardProvider` to avoid changing runtime behavior before the data model and card math are stable.

This iteration does not redesign the dashboard. Layout coordinates remain compatible with the current mockup fidelity goal.
