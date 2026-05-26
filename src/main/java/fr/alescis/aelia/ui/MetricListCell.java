package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.WeatherMetric;
import javafx.scene.control.ListCell;

/**
 * List cell for supported metrics in diagnostic views.
 */
public final class MetricListCell extends ListCell<WeatherMetric> {
    @Override
    protected void updateItem(WeatherMetric metric, boolean empty) {
        super.updateItem(metric, empty);
        if (empty || metric == null) {
            setText(null);
        } else {
            setText(metric.label() + " · " + metric.category() + " · " + metric.supportedPeriod());
        }
    }
}
