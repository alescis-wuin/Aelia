# Build fix V0.3.3

This release restores compatibility helpers used by dashboard-oriented UI code.

## Fixed symbols

- `ApiLimit.limited(String, LimitPeriod, int, String)`
- `ApiLimit.unmetered(String, LimitPeriod, String)`
- `WeatherMetric.maximum()`
- `WeatherMetric.minimum()`
- `LimitPeriod` enum

These helpers keep the simple immutable records while allowing UI code to express provider limits and metric ranges with readable factory methods.
