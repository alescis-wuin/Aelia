# V0.3.12 interactivity restoration

This iteration restores dashboard interactions that were lost during the API-readiness refactoring and keeps the existing visual mockup intact at startup.

## Fixed regressions

- Hourly forecast chips are selectable again.
- The hourly forecast card can be scrolled horizontally with the mouse wheel.
- The hourly forecast card can be panned horizontally by mouse drag.
- Daily forecast rows are selectable again.
- Long-hover tooltips are installed on interactive cards and graph elements.

## Added interactions

- Bottom navigation tabs are keyboard and mouse accessible.
- The Home tab returns to the dashboard.
- The Map and Settings tabs open an "En cours" view without destroying the dashboard state.
- The location search field filters saved locations locally.
- Pressing Enter in the location search selects the first visible result.
- Pressing Escape clears the location search.
- Selecting a saved location updates the main mock dashboard with the selected city and summary temperature while keeping the current deterministic simulated data model.

## Graph hover behavior

- Circular, semi-circular, UV, pollen-bar and solar-path elements expose value tooltips.
- Temperature curve points expose value tooltips.
- Temperature curve points display vertical and horizontal crosshair guides while hovered.

## Scope decision

The location search intentionally remains local to saved simulated locations. Remote geocoding is deferred to the future remote provider phase so this version does not introduce network access, API keys, quotas or latency into the current mockup build.
