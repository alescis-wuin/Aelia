package fr.alescis.aelia.model;

import java.util.UUID;

/**
 * Immutable identifier for an active subscription.
 */
public record SubscriptionHandle(UUID id, String metricId) {
    public SubscriptionHandle {
        if (id == null) {
            throw new IllegalArgumentException("Subscription id is required.");
        }
        if (metricId == null || metricId.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
    }
}
