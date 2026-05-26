package fr.alescis.aelia.model;

import java.time.Duration;

/**
 * Request used to subscribe to periodic metric updates.
 */
public record SubscriptionRequest(String metricId, Duration interval) {
    public SubscriptionRequest {
        if (metricId == null || metricId.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
        if (interval == null || interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Interval must be strictly positive.");
        }
    }
}
