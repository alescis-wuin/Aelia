package fr.alescis.aelia.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Generic typed metric value used by provider composition code.
 */
public record MetricValue(String metricId, double value, String unit, Instant measuredAt, String textValue) {
    public MetricValue {
        if (metricId == null || metricId.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
        unit = unit == null ? "" : unit.trim();
        measuredAt = Objects.requireNonNull(measuredAt, "measuredAt");
        textValue = textValue == null ? "" : textValue.trim();
    }

    public MetricValue(String metricId, double value, String unit, Instant measuredAt) {
        this(metricId, value, unit, measuredAt, "");
    }

    public static MetricValue numeric(DataMetric metric, Instant measuredAt, double value) {
        Objects.requireNonNull(metric, "metric");
        return new MetricValue(metric.id(), value, metric.unit(), measuredAt);
    }

    public static MetricValue text(DataMetric metric, Instant measuredAt, String value) {
        Objects.requireNonNull(metric, "metric");
        return new MetricValue(metric.id(), Double.NaN, metric.unit(), measuredAt, value);
    }

    public DataMetric metric() {
        return new DataMetric(metricId, metricId, DataCategory.WEATHER, textValue.isBlank() ? DataKind.NUMERIC : DataKind.TEXT, unit);
    }

    public Instant timestamp() {
        return measuredAt;
    }
}
