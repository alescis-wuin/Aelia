package fr.seynax.aelia.model;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Current or streamed value for a metric.
 */
public record MetricValue(
        DataMetric metric,
        Instant timestamp,
        OptionalDouble numericValue,
        String textValue,
        double confidence
) {
    public MetricValue {
        Objects.requireNonNull(metric, "metric");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(numericValue, "numericValue");
        Objects.requireNonNull(textValue, "textValue");
        if (textValue.isBlank()) {
            throw new IllegalArgumentException("Metric text value must not be blank.");
        }
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1.");
        }
    }

    public static MetricValue numeric(DataMetric metric, Instant timestamp, double value, double confidence) {
        String formatted = String.format(Locale.ROOT, "%.1f %s", value, metric.unit()).trim();
        return new MetricValue(metric, timestamp, OptionalDouble.of(value), formatted, confidence);
    }

    public static MetricValue text(DataMetric metric, Instant timestamp, String value, double confidence) {
        return new MetricValue(metric, timestamp, OptionalDouble.empty(), value, confidence);
    }
}
