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
        List<ApiLimit> apiLimits,
        DashboardDataStatus dataStatus
) {
    public DashboardSnapshot {
        locations = List.copyOf(Objects.requireNonNull(locations, "locations"));
        currentWeather = Objects.requireNonNull(currentWeather, "currentWeather");
        hourlyForecasts = List.copyOf(Objects.requireNonNull(hourlyForecasts, "hourlyForecasts"));
        dailyForecasts = List.copyOf(Objects.requireNonNull(dailyForecasts, "dailyForecasts"));
        pollenRisks = List.copyOf(Objects.requireNonNull(pollenRisks, "pollenRisks"));
        supportedMetrics = List.copyOf(Objects.requireNonNull(supportedMetrics, "supportedMetrics"));
        apiLimits = List.copyOf(Objects.requireNonNull(apiLimits, "apiLimits"));
        dataStatus = dataStatus == null ? DashboardDataStatus.simulated("simulated") : dataStatus;
    }

    /**
     * Compatibility constructor used by simulated and older provider code.
     */
    public DashboardSnapshot(
            List<LocationWeather> locations,
            CurrentWeather currentWeather,
            List<HourlyForecast> hourlyForecasts,
            List<DailyForecast> dailyForecasts,
            List<PollenRisk> pollenRisks,
            List<WeatherMetric> supportedMetrics,
            List<ApiLimit> apiLimits
    ) {
        this(locations, currentWeather, hourlyForecasts, dailyForecasts, pollenRisks, supportedMetrics, apiLimits,
                DashboardDataStatus.simulated("simulated"));
    }

    public DashboardSnapshot withDataStatus(DashboardDataStatus status) {
        return new DashboardSnapshot(locations, currentWeather, hourlyForecasts, dailyForecasts, pollenRisks,
                supportedMetrics, apiLimits, status);
    }
}
