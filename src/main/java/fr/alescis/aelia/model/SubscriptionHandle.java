package fr.alescis.aelia.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Handle returned by a provider when a metric subscription is opened.
 */
public record SubscriptionHandle(UUID id, String metricId) {
    public SubscriptionHandle {
        id = Objects.requireNonNull(id, "id");
        if (metricId == null || metricId.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
    }
}
