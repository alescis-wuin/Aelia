package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.DataMetric;
import javafx.scene.control.ListCell;

/**
 * Compact accessible cell displaying metric names and categories.
 */
public final class MetricListCell extends ListCell<DataMetric> {
    @Override
    protected void updateItem(DataMetric item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setAccessibleText(null);
            return;
        }
        setText(item.displayName() + " · " + item.category().label());
        setAccessibleText(item.displayName() + ", " + item.description());
    }
}
