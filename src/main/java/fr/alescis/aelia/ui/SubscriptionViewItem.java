package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.SubscriptionSnapshot;

/**
 * Readable item used by future subscription diagnostics.
 */
public record SubscriptionViewItem(SubscriptionSnapshot snapshot) {
    @Override
    public String toString() {
        String lastUpdate = snapshot.lastUpdateAt()
                .map(Instant -> "updated " + Instant)
                .orElse("no update yet");
        return snapshot.metricId() + " · " + snapshot.interval() + " · " + lastUpdate;
    }
}
