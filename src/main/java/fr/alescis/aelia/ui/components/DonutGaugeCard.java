package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Small circular gauge card for humidity and related compact metrics.
 */
public final class DonutGaugeCard extends CardPane {
    public DonutGaugeCard(
            String title,
            String value,
            String unit,
            double normalizedProgress,
            Color accent,
            String status,
            String rightHint,
            Node icon,
            String accessibleText
    ) {
        super(162, 164);
        Label section = UiText.section(title);
        section.setLayoutX(18);
        section.setLayoutY(16);
        getChildren().add(section);

        if (icon != null) {
            icon.setLayoutX(132);
            icon.setLayoutY(16);
            getChildren().add(icon);
        }

        double progressValue = GaugeMath.clamp(normalizedProgress, 0.0, 1.0);
        double centerX = 81.0;
        double centerY = 84.0;
        double radius = 36.0;
        Circle base = new Circle(centerX, centerY, radius);
        base.setFill(Color.TRANSPARENT);
        base.setStroke(Palette.BORDER_SOFT);
        base.setStrokeWidth(8.0);

        Arc progress = new Arc(centerX, centerY, radius, radius, 90, -360.0 * progressValue);
        progress.setFill(Color.TRANSPARENT);
        progress.setStroke(accent);
        progress.setStrokeWidth(8.0);
        progress.setStrokeLineCap(StrokeLineCap.ROUND);
        progress.setType(ArcType.OPEN);

        Label valueLabel = UiText.data(value, "donut-value");
        Label unitLabel = UiText.data(unit, "donut-unit");
        VBox centerText = centeredValueBlock(valueLabel, unitLabel, centerX, centerY, 78, 52);

        Label statusLabel = UiText.label(status, "metric-status");
        statusLabel.setTextFill(accent);
        statusLabel.setLayoutX(18);
        statusLabel.setLayoutY(135);

        Label hintLabel = UiText.label(rightHint, "metric-hint");
        hintLabel.setLayoutX(66);
        hintLabel.setLayoutY(135);
        hintLabel.setPrefWidth(80);

        getChildren().addAll(base, progress, centerText, statusLabel, hintLabel);
        String tooltipText = title + " : " + value + (unit.isBlank() ? "" : " " + unit) + " · " + status + " · " + rightHint;
        TooltipSupport.install(this, tooltipText);
        AccessibilitySupport.describe(this, AccessibleRole.TEXT, accessibleText, "Carte de mesure avec jauge circulaire.");
    }

    private static VBox centeredValueBlock(Label value, Label unit, double centerX, double centerY, double width, double height) {
        value.setMaxWidth(Double.MAX_VALUE);
        unit.setMaxWidth(Double.MAX_VALUE);
        value.setAlignment(Pos.CENTER);
        unit.setAlignment(Pos.CENTER);
        VBox box = new VBox(-2, value, unit);
        box.setAlignment(Pos.CENTER);
        box.setPrefSize(width, height);
        box.setMinSize(width, height);
        box.setMaxSize(width, height);
        box.setLayoutX(centerX - width / 2.0);
        box.setLayoutY(centerY - height / 2.0);
        box.setMouseTransparent(true);
        return box;
    }
}
