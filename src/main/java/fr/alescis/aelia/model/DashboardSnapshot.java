package fr.alescis.aelia.model;

import java.util.List;

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
        locations = List.copyOf(locations);
        hourlyForecasts = List.copyOf(hourlyForecasts);
        dailyForecasts = List.copyOf(dailyForecasts);
        pollenRisks = List.copyOf(pollenRisks);
        supportedMetrics = List.copyOf(supportedMetrics);
        apiLimits = List.copyOf(apiLimits);
        if (currentWeather == null) {
            throw new IllegalArgumentException("Current weather is required.");
        }
    }
}
