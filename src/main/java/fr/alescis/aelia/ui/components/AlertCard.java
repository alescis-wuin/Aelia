package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Flat actionable alert card.
 */
public final class AlertCard extends CardPane {
    public AlertCard(String advice) {
        super(522, 52);
        getStyleClass().add("alert-card");
        var icon = WeatherIcons.alertTriangle();
        icon.setLayoutX(14);
        icon.setLayoutY(14);
        Label exclamation = UiText.data("!", "alert-icon-text");
        exclamation.setLayoutX(20);
        exclamation.setLayoutY(14);

        Label title = UiText.label("Alerte UV élevée", "alert-title");
        title.setLayoutX(52);
        title.setLayoutY(10);
        Label body = UiText.label(advice, "alert-body");
        body.setLayoutX(52);
        body.setLayoutY(28);

        Button details = new Button("Voir détails →");
        details.getStyleClass().add("alert-link");
        details.setLayoutX(410);
        details.setLayoutY(12);
        details.setFocusTraversable(true);
        details.setAccessibleText("Voir les détails de l'alerte ultraviolet");

        getChildren().addAll(icon, exclamation, title, body, details);
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, "Alerte ultraviolet élevée", advice);
    }
}
