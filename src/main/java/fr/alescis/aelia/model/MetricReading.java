package fr.alescis.aelia.model;

import java.time.Instant;

/**
 * Value returned for an on-demand request or a subscription update.
 */
public record MetricReading(String metricId, double value, String unit, Instant measuredAt) {
    public MetricReading {
        if (metricId == null || metricId.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
        if (unit == null) {
            throw new IllegalArgumentException("Metric unit is required.");
        }
        if (measuredAt == null) {
            throw new IllegalArgumentException("Measurement timestamp is required.");
        }
    }
}
