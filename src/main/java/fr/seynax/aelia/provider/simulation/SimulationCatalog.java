package fr.seynax.aelia.provider.simulation;

import fr.seynax.aelia.model.ApiLimit;
import fr.seynax.aelia.model.DataCategory;
import fr.seynax.aelia.model.DataKind;
import fr.seynax.aelia.model.DataMetric;
import fr.seynax.aelia.model.LimitPeriod;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Static catalog used by the local weather simulator.
 */
final class SimulationCatalog {

    private static final DataMetric WEATHER_CONDITION = text(
            "weather_condition",
            "Weather condition",
            DataCategory.WEATHER,
            "Stable textual state such as clear sky, overcast or rain."
    );

    private static final DataMetric SUNRISE = text(
            "sunrise_time",
            "Sunrise",
            DataCategory.ASTRONOMY,
            "Approximate local sunrise time produced by the simulator."
    );

    private static final DataMetric SUNSET = text(
            "sunset_time",
            "Sunset",
            DataCategory.ASTRONOMY,
            "Approximate local sunset time produced by the simulator."
    );

    private static final List<MetricProfile> NUMERIC_PROFILES = List.of(
            profile(metric("temperature_air", "Air temperature", DataCategory.WEATHER, "°C", -12.0, 42.0,
                            "Air temperature, comparable to a shaded standard measurement."), 7.0, 23.0, 4.0, 30.0, 0.010, 0.020, 0.018),
            profile(metric("apparent_temperature", "Apparent temperature", DataCategory.WEATHER, "°C", -18.0, 48.0,
                            "Human-perceived temperature influenced by humidity and wind."), 5.0, 26.0, 0.0, 34.0, 0.013, 0.022, 0.020),
            profile(metric("cloud_cover", "Cloud cover", DataCategory.WEATHER, "%", 0.0, 100.0,
                            "Estimated sky coverage by clouds."), 25.0, 75.0, 10.0, 90.0, 0.030, 0.018, 0.045),
            profile(metric("wind_speed", "Wind speed", DataCategory.WEATHER, "km/h", 0.0, 95.0,
                            "Sustained wind speed near ground level."), 4.0, 28.0, 0.0, 45.0, 0.026, 0.026, 0.040),
            profile(metric("wind_gust", "Wind gust", DataCategory.WEATHER, "km/h", 0.0, 130.0,
                            "Short wind gust estimate."), 12.0, 48.0, 0.0, 75.0, 0.040, 0.030, 0.075),
            profile(metric("relative_humidity", "Relative humidity", DataCategory.WEATHER, "%", 10.0, 100.0,
                            "Relative humidity near ground level."), 45.0, 90.0, 35.0, 95.0, 0.025, 0.020, 0.035),
            profile(metric("precipitation_rate", "Precipitation rate", DataCategory.WEATHER, "mm/h", 0.0, 55.0,
                            "Instantaneous rain, snow or mixed precipitation intensity."), 0.0, 2.5, 0.0, 12.0, 0.050, 0.035, 0.065),
            profile(metric("daily_precipitation", "Daily precipitation", DataCategory.WEATHER, "mm", 0.0, 95.0,
                            "Estimated daily accumulated precipitation."), 0.0, 8.0, 0.0, 35.0, 0.030, 0.018, 0.035),
            profile(metric("uv_index", "UV index", DataCategory.WEATHER, "", 0.0, 11.0,
                            "Ultraviolet index on a standard public scale."), 0.0, 7.0, 0.0, 9.0, 0.010, 0.018, 0.012),
            profile(metric("european_aqi", "European AQI", DataCategory.AIR_QUALITY, "", 0.0, 100.0,
                            "European air-quality index approximation."), 12.0, 55.0, 5.0, 75.0, 0.020, 0.018, 0.030),
            profile(metric("pm2_5", "PM2.5", DataCategory.AIR_QUALITY, "µg/m³", 0.0, 85.0,
                            "Fine particles below 2.5 micrometers."), 3.0, 28.0, 0.0, 45.0, 0.022, 0.022, 0.030),
            profile(metric("pm10", "PM10", DataCategory.AIR_QUALITY, "µg/m³", 0.0, 130.0,
                            "Particles below 10 micrometers."), 8.0, 45.0, 0.0, 75.0, 0.022, 0.022, 0.038),
            profile(metric("nitrogen_dioxide", "Nitrogen dioxide", DataCategory.AIR_QUALITY, "µg/m³", 0.0, 220.0,
                            "Nitrogen dioxide concentration."), 8.0, 65.0, 0.0, 120.0, 0.028, 0.020, 0.045),
            profile(metric("ozone", "Ozone", DataCategory.AIR_QUALITY, "µg/m³", 0.0, 260.0,
                            "Near-surface ozone concentration."), 25.0, 115.0, 5.0, 170.0, 0.018, 0.016, 0.040),
            profile(metric("grass_pollen", "Grass pollen", DataCategory.POLLEN, "grains/m³", 0.0, 260.0,
                            "Grass pollen concentration."), 0.0, 95.0, 0.0, 180.0, 0.018, 0.015, 0.028),
            profile(metric("birch_pollen", "Birch pollen", DataCategory.POLLEN, "grains/m³", 0.0, 220.0,
                            "Birch pollen concentration."), 0.0, 80.0, 0.0, 160.0, 0.018, 0.015, 0.028),
            profile(metric("alder_pollen", "Alder pollen", DataCategory.POLLEN, "grains/m³", 0.0, 180.0,
                            "Alder pollen concentration."), 0.0, 65.0, 0.0, 120.0, 0.018, 0.015, 0.024),
            profile(metric("ragweed_pollen", "Ragweed pollen", DataCategory.POLLEN, "grains/m³", 0.0, 170.0,
                            "Ragweed pollen concentration."), 0.0, 50.0, 0.0, 110.0, 0.018, 0.015, 0.022),
            profile(metric("olive_pollen", "Olive pollen", DataCategory.POLLEN, "grains/m³", 0.0, 190.0,
                            "Olive pollen concentration."), 0.0, 45.0, 0.0, 120.0, 0.016, 0.014, 0.022),
            profile(metric("mugwort_pollen", "Mugwort pollen", DataCategory.POLLEN, "grains/m³", 0.0, 160.0,
                            "Mugwort pollen concentration."), 0.0, 45.0, 0.0, 100.0, 0.016, 0.014, 0.020)
    );

    private static final List<DataMetric> TEXT_METRICS = List.of(WEATHER_CONDITION, SUNRISE, SUNSET);

    private static final List<DataMetric> ALL_METRICS = buildMetrics();

    private static final Map<String, DataMetric> METRICS_BY_ID = ALL_METRICS.stream()
            .collect(Collectors.toUnmodifiableMap(DataMetric::id, Function.identity()));

    private SimulationCatalog() {
    }

    static List<MetricProfile> numericProfiles() {
        return NUMERIC_PROFILES;
    }

    static List<DataMetric> metrics() {
        return ALL_METRICS;
    }

    static DataMetric metricById(String metricId) {
        DataMetric metric = METRICS_BY_ID.get(metricId);
        if (metric == null) {
            throw new IllegalArgumentException("Unsupported metric: " + metricId);
        }
        return metric;
    }

    static DataMetric weatherConditionMetric() {
        return WEATHER_CONDITION;
    }

    static DataMetric sunriseMetric() {
        return SUNRISE;
    }

    static DataMetric sunsetMetric() {
        return SUNSET;
    }

    static List<ApiLimit> limits() {
        return List.of(
                new ApiLimit("Current value", LimitPeriod.LOCAL, OptionalLong.empty(), "request", "In-process simulator, no remote quota."),
                new ApiLimit("Subscription interval", LimitPeriod.LOCAL, OptionalLong.of(1), "second minimum", "Intervals below one second are rejected by the service."),
                new ApiLimit("Recommended subscriptions", LimitPeriod.LOCAL, OptionalLong.of(20), "active streams", "Soft limit to keep the UI readable."),
                new ApiLimit("Remote API calls", LimitPeriod.DAY, OptionalLong.of(0), "network request", "No remote API is used in V0.1.")
        );
    }

    private static List<DataMetric> buildMetrics() {
        return java.util.stream.Stream.concat(
                        NUMERIC_PROFILES.stream().map(MetricProfile::metric),
                        TEXT_METRICS.stream()
                )
                .sorted((left, right) -> {
                    int categoryOrder = left.category().compareTo(right.category());
                    if (categoryOrder != 0) {
                        return categoryOrder;
                    }
                    return left.displayName().compareToIgnoreCase(right.displayName());
                })
                .toList();
    }

    private static MetricProfile profile(
            DataMetric metric,
            double initialMinimum,
            double initialMaximum,
            double targetMinimum,
            double targetMaximum,
            double volatility,
            double attraction,
            double maximumStepPerSecond
    ) {
        return new MetricProfile(metric, initialMinimum, initialMaximum, targetMinimum, targetMaximum,
                volatility, attraction, maximumStepPerSecond);
    }

    private static DataMetric metric(String id, String displayName, DataCategory category, String unit,
                                     double minimum, double maximum, String description) {
        return new DataMetric(id, displayName, category, DataKind.NUMERIC, unit, minimum, maximum, description);
    }

    private static DataMetric text(String id, String displayName, DataCategory category, String description) {
        return new DataMetric(id, displayName, category, DataKind.TEXT, "", Double.NaN, Double.NaN, description);
    }
}
