package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DataCategory;
import fr.alescis.aelia.model.DataKind;
import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.LimitPeriod;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Static simulated-provider catalog used by the application and tests.
 */
public final class SimulationCatalog {
    private static final List<DataMetric> METRICS = List.of(
            DataMetric.numeric(
                    "air_temperature",
                    "Air temperature",
                    DataCategory.WEATHER,
                    "°C",
                    -20.0d,
                    44.0d,
                    1,
                    "Standard air temperature, comparable to a shaded measurement."
            ),
            DataMetric.numeric(
                    "sun_temperature_estimate",
                    "Sun exposure estimate",
                    DataCategory.WEATHER,
                    "°C",
                    -15.0d,
                    58.0d,
                    1,
                    "Estimated outdoor thermal perception under direct sun exposure."
            ),
            DataMetric.numeric(
                    "apparent_temperature",
                    "Feels like",
                    DataCategory.WEATHER,
                    "°C",
                    -28.0d,
                    52.0d,
                    1,
                    "Perceived temperature derived from simulated humidity and wind."
            ),
            DataMetric.text(
                    "condition",
                    "Condition",
                    DataCategory.WEATHER,
                    DataKind.TEXT,
                    "Compact text summary of the current simulated sky state."
            ),
            DataMetric.numeric(
                    "wind_speed",
                    "Wind",
                    DataCategory.ATMOSPHERE,
                    "km/h",
                    0.0d,
                    130.0d,
                    0,
                    "Near-surface wind speed."
            ),
            DataMetric.numeric(
                    "humidity",
                    "Humidity",
                    DataCategory.ATMOSPHERE,
                    "%",
                    10.0d,
                    100.0d,
                    0,
                    "Relative humidity near ground level."
            ),
            DataMetric.numeric(
                    "precipitation_rate",
                    "Rain rate",
                    DataCategory.WEATHER,
                    "mm/h",
                    0.0d,
                    40.0d,
                    1,
                    "Instant precipitation intensity."
            ),
            DataMetric.numeric(
                    "cloud_cover",
                    "Cloud cover",
                    DataCategory.ATMOSPHERE,
                    "%",
                    0.0d,
                    100.0d,
                    0,
                    "Sky coverage by clouds."
            ),
            DataMetric.text(
                    "sunrise",
                    "Sunrise",
                    DataCategory.SUN,
                    DataKind.TEMPORAL,
                    "Approximate local sunrise time."
            ),
            DataMetric.text(
                    "sunset",
                    "Sunset",
                    DataCategory.SUN,
                    DataKind.TEMPORAL,
                    "Approximate local sunset time."
            ),
            DataMetric.numeric(
                    "uv_index",
                    "UV index",
                    DataCategory.SUN,
                    "UVI",
                    0.0d,
                    11.0d,
                    1,
                    "Ultraviolet exposure index."
            ),
            DataMetric.numeric(
                    "pm25",
                    "PM2.5",
                    DataCategory.AIR,
                    "µg/m³",
                    0.0d,
                    90.0d,
                    1,
                    "Fine particulate matter concentration."
            ),
            DataMetric.numeric(
                    "european_aqi",
                    "European AQI",
                    DataCategory.AIR,
                    "AQI",
                    0.0d,
                    150.0d,
                    0,
                    "European air quality index."
            ),
            DataMetric.numeric(
                    "grass_pollen",
                    "Grass pollen",
                    DataCategory.HEALTH,
                    "grains/m³",
                    0.0d,
                    350.0d,
                    0,
                    "Grass pollen concentration estimate."
            )
    );

    private static final Map<String, DataMetric> METRICS_BY_ID = METRICS.stream()
            .collect(Collectors.toUnmodifiableMap(DataMetric::id, Function.identity()));

    private SimulationCatalog() {
    }

    public static List<DataMetric> metrics() {
        return METRICS;
    }

    public static Map<String, DataMetric> metricsById() {
        return METRICS_BY_ID;
    }

    public static List<ApiLimit> limits() {
        return List.of(
                ApiLimit.limited("Current value reads", LimitPeriod.SECOND, 4, "Local guard matching a modest remote API."),
                ApiLimit.limited("Current value reads", LimitPeriod.MINUTE, 120, "Enough for manual UI usage and light polling."),
                ApiLimit.limited("Active streams", LimitPeriod.STREAM, 8, "Maximum simulated subscriptions kept in memory."),
                ApiLimit.limited("Minimum stream interval", LimitPeriod.SECOND, 1, "The service rejects sub-second subscriptions."),
                ApiLimit.unmetered("Network calls", LimitPeriod.DAY, "No network request is performed by the simulated provider.")
        );
    }

    static List<MetricProfile> profiles() {
        return List.of(
                profile("air_temperature", 18.0d, -20.0d, 44.0d, 0.025d, 0.0028d, 6.0d, Duration.ofHours(24)),
                profile("sun_temperature_estimate", 24.0d, -15.0d, 58.0d, 0.035d, 0.0032d, 11.0d, Duration.ofHours(24)),
                profile("apparent_temperature", 17.0d, -28.0d, 52.0d, 0.030d, 0.0025d, 7.0d, Duration.ofHours(24)),
                profile("wind_speed", 18.0d, 0.0d, 130.0d, 0.110d, 0.0040d, 8.0d, Duration.ofHours(8)),
                profile("humidity", 64.0d, 10.0d, 100.0d, 0.180d, 0.0030d, 18.0d, Duration.ofHours(12)),
                profile("precipitation_rate", 1.2d, 0.0d, 40.0d, 0.070d, 0.0120d, 2.5d, Duration.ofHours(6)),
                profile("cloud_cover", 48.0d, 0.0d, 100.0d, 0.250d, 0.0038d, 28.0d, Duration.ofHours(10)),
                profile("uv_index", 2.6d, 0.0d, 11.0d, 0.020d, 0.0050d, 4.8d, Duration.ofHours(24)),
                profile("pm25", 12.0d, 0.0d, 90.0d, 0.080d, 0.0040d, 5.5d, Duration.ofHours(18)),
                profile("european_aqi", 34.0d, 0.0d, 150.0d, 0.120d, 0.0035d, 15.0d, Duration.ofHours(18)),
                profile("grass_pollen", 58.0d, 0.0d, 350.0d, 0.320d, 0.0024d, 70.0d, Duration.ofHours(24))
        );
    }

    private static MetricProfile profile(
            String metricId,
            double base,
            double minimum,
            double maximum,
            double volatility,
            double recovery,
            double cycleAmplitude,
            Duration cycleDuration
    ) {
        return new MetricProfile(metric(metricId), base, minimum, maximum, volatility, recovery, cycleAmplitude, cycleDuration);
    }

    private static DataMetric metric(String id) {
        DataMetric metric = METRICS_BY_ID.get(id);
        if (metric == null) {
            throw new IllegalStateException("missing simulated metric: " + id);
        }
        return metric;
    }
}
