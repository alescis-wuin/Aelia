package fr.alescis.aelia.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Value returned for an on-demand request or a subscription update.
 */
public record MetricReading(String metricId, double value, String unit, Instant measuredAt) {
    public MetricReading {
        if (metricId == null || metricId.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
        unit = unit == null ? "" : unit;
        measuredAt = Objects.requireNonNull(measuredAt, "measuredAt");
    }
}
