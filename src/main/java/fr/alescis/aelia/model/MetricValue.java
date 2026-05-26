package fr.alescis.aelia.model;

import java.time.Instant;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Immutable value returned by a weather data provider.
 */
public record MetricValue(
        DataMetric metric,
        Instant timestamp,
        OptionalDouble numericValue,
        String textValue
) {
    public MetricValue {
        metric = Objects.requireNonNull(metric, "metric");
        timestamp = Objects.requireNonNull(timestamp, "timestamp");
        numericValue = Objects.requireNonNull(numericValue, "numericValue");
        textValue = Objects.requireNonNull(textValue, "textValue").trim();
        if (metric.kind() == DataKind.NUMERIC && numericValue.isEmpty()) {
            throw new IllegalArgumentException("numeric metrics require a numeric value");
        }
        if (metric.kind() != DataKind.NUMERIC && textValue.isBlank()) {
            throw new IllegalArgumentException("text metrics require a text value");
        }
    }

    public static MetricValue numeric(DataMetric metric, Instant timestamp, double value) {
        return new MetricValue(metric, timestamp, OptionalDouble.of(value), "");
    }

    public static MetricValue text(DataMetric metric, Instant timestamp, String value) {
        return new MetricValue(metric, timestamp, OptionalDouble.empty(), value);
    }
}
