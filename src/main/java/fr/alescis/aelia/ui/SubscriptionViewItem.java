package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.time.Duration;
import java.util.UUID;

/**
 * JavaFX table row model for an active subscription.
 */
public final class SubscriptionViewItem {
    private final UUID id;
    private final DataMetric metric;
    private final Duration interval;
    private final ReadOnlyStringWrapper metricName;
    private final ReadOnlyStringWrapper intervalText;
    private final ReadOnlyStringWrapper lastValue;
    private final ReadOnlyStringWrapper lastUpdate;

    public SubscriptionViewItem(UUID id, DataMetric metric, Duration interval, MetricValue initialValue) {
        this.id = id;
        this.metric = metric;
        this.interval = interval;
        this.metricName = new ReadOnlyStringWrapper(metric.displayName());
        this.intervalText = new ReadOnlyStringWrapper(UiFormatters.interval(interval));
        this.lastValue = new ReadOnlyStringWrapper(UiFormatters.value(initialValue));
        this.lastUpdate = new ReadOnlyStringWrapper(UiFormatters.timestamp(initialValue.timestamp()));
    }

    public UUID id() {
        return id;
    }

    public DataMetric metric() {
        return metric;
    }

    public Duration interval() {
        return interval;
    }

    public ReadOnlyStringProperty metricNameProperty() {
        return metricName.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty intervalTextProperty() {
        return intervalText.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty lastValueProperty() {
        return lastValue.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty lastUpdateProperty() {
        return lastUpdate.getReadOnlyProperty();
    }

    public void update(MetricValue value) {
        lastValue.set(UiFormatters.value(value));
        lastUpdate.set(UiFormatters.timestamp(value.timestamp()));
    }
}
