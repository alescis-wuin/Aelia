package fr.seynax.aelia.ui;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JavaFX-friendly representation of an active subscription row.
 */
public final class SubscriptionViewItem {

    private final UUID id;
    private final ReadOnlyStringWrapper metricName = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper interval = new ReadOnlyStringWrapper();
    private final ReadOnlyStringWrapper lastValue = new ReadOnlyStringWrapper("waiting");
    private final ReadOnlyStringWrapper updatedAt = new ReadOnlyStringWrapper("not received");

    public SubscriptionViewItem(UUID id, String metricName, Duration interval) {
        this.id = Objects.requireNonNull(id, "id");
        this.metricName.set(Objects.requireNonNull(metricName, "metricName"));
        this.interval.set(UiFormatters.duration(Objects.requireNonNull(interval, "interval")));
    }

    public UUID id() {
        return id;
    }

    public ReadOnlyStringProperty metricNameProperty() {
        return metricName.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty intervalProperty() {
        return interval.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty lastValueProperty() {
        return lastValue.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty updatedAtProperty() {
        return updatedAt.getReadOnlyProperty();
    }

    public void update(String value, Instant timestamp) {
        lastValue.set(Objects.requireNonNull(value, "value"));
        updatedAt.set(UiFormatters.instant(Objects.requireNonNull(timestamp, "timestamp")));
    }
}
