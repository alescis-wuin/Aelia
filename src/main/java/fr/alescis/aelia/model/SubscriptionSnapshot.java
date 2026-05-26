package fr.alescis.aelia.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable state exposed for a provider subscription.
 */
public record SubscriptionSnapshot(
        UUID id,
        String metricId,
        Duration interval,
        Instant createdAt,
        Optional<Instant> lastUpdateAt
) {
    public SubscriptionSnapshot {
        id = Objects.requireNonNull(id, "id");
        metricId = requireText(metricId, "metricId");
        interval = Objects.requireNonNull(interval, "interval");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        lastUpdateAt = Objects.requireNonNull(lastUpdateAt, "lastUpdateAt");
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval must be positive");
        }
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
