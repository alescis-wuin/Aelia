package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Wind card with speed gauge, direction badge and gust value.
 */
public final class WindCard extends CardPane {
    public WindCard(int speedKmh, String direction, int gustKmh) {
        super(162, 164);
        Label title = UiText.section("Vent");
        title.setLayoutX(18);
        title.setLayoutY(16);

        var windMark = WeatherIcons.windMark();
        windMark.setLayoutX(107);
        windMark.setLayoutY(13);

        double centerX = 81.0;
        double centerY = 68.0;
        double radius = 34.0;
        Circle base = new Circle(centerX, centerY, radius);
        base.setFill(null);
        base.setStroke(Palette.BORDER_SOFT);
        base.setStrokeWidth(8.0);

        Arc progress = new Arc(centerX, centerY, radius, radius, 90, -360.0 * 0.18);
        progress.setFill(null);
        progress.setStroke(Palette.LIME);
        progress.setStrokeWidth(8.0);
        progress.setStrokeLineCap(StrokeLineCap.ROUND);
        progress.setType(ArcType.OPEN);

        Label speed = UiText.data(String.valueOf(speedKmh), "donut-value");
        Label unit = UiText.data("km/h", "donut-unit");
        VBox centerText = centeredValueBlock(speed, unit, centerX, centerY, 76, 52);

        Rectangle badge = new Rectangle(98, 20);
        badge.setLayoutX(32);
        badge.setLayoutY(114);
        badge.setArcWidth(20);
        badge.setArcHeight(20);
        badge.getStyleClass().add("wind-direction-badge");

        Label directionLabel = UiText.data(direction, "wind-direction");
        directionLabel.setLayoutX(32);
        directionLabel.setLayoutY(117);
        directionLabel.setPrefWidth(98);
        directionLabel.setAlignment(Pos.CENTER);

        Label gustLabel = UiText.label("Rafales", "metric-hint-left");
        gustLabel.setLayoutX(18);
        gustLabel.setLayoutY(146);
        Label gustValue = UiText.data(gustKmh + " km/h", "metric-hint-value");
        gustValue.setLayoutX(88);
        gustValue.setLayoutY(146);
        gustValue.setPrefWidth(58);
        gustValue.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(title, windMark, base, progress, centerText, badge, directionLabel, gustLabel, gustValue);
        AccessibilitySupport.describe(this, AccessibleRole.TEXT, "Vent " + speedKmh + " kilomètres par heure, direction " + direction + ", rafales " + gustKmh + " kilomètres par heure", "Carte de vitesse du vent, direction et rafales.");
    }

    private static VBox centeredValueBlock(Label value, Label unit, double centerX, double centerY, double width, double height) {
        value.setAlignment(Pos.CENTER);
        unit.setAlignment(Pos.CENTER);
        value.setMaxWidth(Double.MAX_VALUE);
        unit.setMaxWidth(Double.MAX_VALUE);
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
