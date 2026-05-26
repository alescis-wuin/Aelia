package fr.alescis.aelia.model;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated read model consumed by the JavaFX dashboard.
 */
public record DashboardSnapshot(
        List<LocationWeather> locations,
        CurrentWeather currentWeather,
        List<HourlyForecast> hourlyForecasts,
        List<DailyForecast> dailyForecasts,
        List<PollenRisk> pollenRisks,
        List<WeatherMetric> supportedMetrics,
        List<ApiLimit> apiLimits
) {
    public DashboardSnapshot {
        locations = List.copyOf(Objects.requireNonNull(locations, "locations"));
        currentWeather = Objects.requireNonNull(currentWeather, "currentWeather");
        hourlyForecasts = List.copyOf(Objects.requireNonNull(hourlyForecasts, "hourlyForecasts"));
        dailyForecasts = List.copyOf(Objects.requireNonNull(dailyForecasts, "dailyForecasts"));
        pollenRisks = List.copyOf(Objects.requireNonNull(pollenRisks, "pollenRisks"));
        supportedMetrics = List.copyOf(Objects.requireNonNull(supportedMetrics, "supportedMetrics"));
        apiLimits = List.copyOf(Objects.requireNonNull(apiLimits, "apiLimits"));
    }
}
