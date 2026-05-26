package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.ui.UiFormatters;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Objects;

/**
 * Small, reusable value card used by the dashboard glance row.
 */
public final class MetricTile extends VBox {
    private final DataMetric metric;
    private final Label nameLabel;
    private final Label valueLabel;
    private final Label metaLabel;

    public MetricTile(DataMetric metric) {
        this.metric = Objects.requireNonNull(metric, "metric");
        this.nameLabel = new Label(metric.displayName());
        this.valueLabel = new Label("—");
        this.metaLabel = new Label(metric.unit().isBlank() ? metric.category().label() : metric.unit());
        build();
    }

    public DataMetric metric() {
        return metric;
    }

    public void update(MetricValue value) {
        valueLabel.setText(UiFormatters.shortValue(value));
        metaLabel.setText(value.metric().unit().isBlank() ? UiFormatters.timestamp(value.timestamp()) : value.metric().unit());
        setAccessibleText(value.metric().displayName() + ": " + UiFormatters.value(value));
    }

    private void build() {
        getStyleClass().add("metric-tile");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4.0d);
        setAccessibleRole(AccessibleRole.TEXT);
        setAccessibleText(metric.displayName() + ": no value yet");

        nameLabel.getStyleClass().add("tile-name");
        valueLabel.getStyleClass().add("tile-value");
        metaLabel.getStyleClass().add("tile-meta");
        getChildren().addAll(nameLabel, valueLabel, metaLabel);
    }
}
