package fr.alescis.aelia.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Defines a subscription to a single metric at a fixed interval.
 */
public record SubscriptionRequest(String metricId, Duration interval) {
    public SubscriptionRequest {
        if (metricId == null || metricId.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
        interval = Objects.requireNonNull(interval, "interval");
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Interval must be positive.");
        }
    }
}
