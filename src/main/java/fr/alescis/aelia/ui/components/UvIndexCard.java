package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Segmented UV index scale without gradients, with the related alert embedded in the same card.
 */
public final class UvIndexCard extends CardPane {
    private static final Color[] SEGMENT_COLORS = {Palette.GREEN, Palette.LIME, Palette.YELLOW, Palette.ORANGE, Palette.RED, Palette.VIOLET};
    private static final String[] LABELS = {"Faible", "Modéré", "Élevé", "Très élevé", "Extrême"};

    public UvIndexCard(int uvIndex, String advice) {
        super(522, 124);
        Label title = UiText.section("Indice UV");
        title.setLayoutX(20);
        title.setLayoutY(16);
        getChildren().add(title);

        double x = 20.0;
        for (int segmentIndex = 0; segmentIndex < SEGMENT_COLORS.length; segmentIndex++) {
            Color color = SEGMENT_COLORS[segmentIndex];
            Rectangle segment = new Rectangle(55, 8);
            segment.setLayoutX(x);
            segment.setLayoutY(34);
            segment.setArcWidth(8);
            segment.setArcHeight(8);
            segment.setFill(color);
            TooltipSupport.install(segment, "Indice UV " + uvIndex + " · segment " + (segmentIndex + 1));
            getChildren().add(segment);
            x += 55.0;
        }

        double knobX = 20 + GaugeMath.normalize(uvIndex, 0.0, 11.0) * 330.0;
        Circle knob = new Circle(knobX, 38, 6.5, Palette.TEXT);
        knob.setStroke(Palette.BACKGROUND);
        knob.setStrokeWidth(2.0);
        getChildren().add(knob);
        TooltipSupport.install(knob, "Indice UV actuel : " + uvIndex);

        Label value = UiText.data(String.valueOf(uvIndex), "uv-value");
        value.setLayoutX(364);
        value.setLayoutY(23);
        value.setPrefWidth(44);
        getChildren().add(value);

        double[] labelX = {42, 122, 192, 268, 340};
        for (int index = 0; index < LABELS.length; index++) {
            Label label = UiText.label(LABELS[index], "uv-scale-label");
            label.setLayoutX(labelX[index]);
            label.setLayoutY(51);
            getChildren().add(label);
        }

        buildInlineAlert(advice);
        TooltipSupport.install(this, "Indice UV " + uvIndex + " · " + advice);
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, "Indice ultraviolet " + uvIndex + ". Alerte ultraviolet élevée.", advice);
    }

    private void buildInlineAlert(String advice) {
        Rectangle background = new Rectangle(484, 38);
        background.setLayoutX(20);
        background.setLayoutY(76);
        background.setArcWidth(12);
        background.setArcHeight(12);
        background.getStyleClass().add("uv-inline-alert");

        var icon = WeatherIcons.alertTriangle();
        icon.setLayoutX(32);
        icon.setLayoutY(86);
        Label exclamation = UiText.data("!", "alert-icon-text");
        exclamation.setLayoutX(38);
        exclamation.setLayoutY(86);

        Label title = UiText.label("Alerte UV élevée", "alert-title");
        title.setLayoutX(70);
        title.setLayoutY(82);
        Label body = UiText.label(advice, "alert-body");
        body.setLayoutX(70);
        body.setLayoutY(98);
        body.setPrefWidth(330);

        Button details = new Button("Détails →");
        details.getStyleClass().add("alert-link");
        details.setLayoutX(424);
        details.setLayoutY(90);
        details.setFocusTraversable(true);
        details.setAccessibleText("Voir les détails de l'alerte ultraviolet");

        TooltipSupport.install(background, "Alerte UV élevée · " + advice);
        TooltipSupport.install(details, "Afficher les détails de l'alerte UV");
        getChildren().addAll(background, icon, exclamation, title, body, details);
    }
}
