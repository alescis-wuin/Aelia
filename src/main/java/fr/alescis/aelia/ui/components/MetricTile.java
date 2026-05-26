package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.WeatherMetric;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.UiText;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;

/**
 * Compact metric descriptor tile used by diagnostic or settings views.
 */
public final class MetricTile extends CardPane {
    public MetricTile(WeatherMetric metric) {
        super(210, 72);
        Label title = UiText.label(metric.label(), "forecast-day-active");
        title.setLayoutX(14);
        title.setLayoutY(12);
        Label details = UiText.label(metric.category() + " · " + metric.supportedPeriod(), "location-details");
        details.setLayoutX(14);
        details.setLayoutY(34);
        details.setPrefWidth(180);
        getChildren().addAll(title, details);
        AccessibilitySupport.describe(this, AccessibleRole.TEXT, metric.label(), metric.supportedPeriod());
    }
}
