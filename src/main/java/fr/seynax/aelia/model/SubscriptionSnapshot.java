package fr.seynax.aelia.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only subscription state exposed by a provider.
 */
public record SubscriptionSnapshot(
        UUID id,
        DataMetric metric,
        Duration interval,
        Instant createdAt
) {
    public SubscriptionSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(metric, "metric");
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(createdAt, "createdAt");
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Subscription interval must be positive.");
        }
    }
}
