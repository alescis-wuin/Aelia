package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.UiText;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Reusable alert card for future weather warnings.
 */
public final class AlertCard extends CardPane {
    public AlertCard(String title, String body, String actionLabel) {
        super(522, 54);
        getStyleClass().add("alert-card");
        var icon = WeatherIcons.alertTriangle();
        icon.setLayoutX(20);
        icon.setLayoutY(14);
        Label exclamation = UiText.data("!", "alert-icon-text");
        exclamation.setLayoutX(26);
        exclamation.setLayoutY(14);
        Label titleLabel = UiText.label(title, "alert-title");
        titleLabel.setLayoutX(66);
        titleLabel.setLayoutY(9);
        Label bodyLabel = UiText.label(body, "alert-body");
        bodyLabel.setLayoutX(66);
        bodyLabel.setLayoutY(26);
        Button action = new Button(actionLabel);
        action.getStyleClass().add("alert-link");
        action.setLayoutX(428);
        action.setLayoutY(18);
        action.setFocusTraversable(true);
        getChildren().addAll(icon, exclamation, titleLabel, bodyLabel, action);
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, title, body);
    }
}
