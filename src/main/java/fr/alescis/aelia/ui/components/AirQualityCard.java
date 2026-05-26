package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.AirQuality;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

/**
 * Air-quality index card with key pollutants.
 */
public final class AirQualityCard extends CardPane {
    public AirQualityCard(AirQuality airQuality) {
        super(256, 254);
        Label title = UiText.section("IQA · Qualité de l'air");
        title.setLayoutX(20);
        title.setLayoutY(19);

        Circle base = new Circle(128, 95, 52);
        base.setFill(null);
        base.setStroke(Palette.BORDER_SOFT);
        base.setStrokeWidth(10.0);

        Arc arc = new Arc(128, 95, 52, 52, 90, -302.0);
        arc.setFill(null);
        arc.setStroke(Palette.LIME);
        arc.setStrokeWidth(10.0);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setType(ArcType.OPEN);

        Label value = UiText.data(String.valueOf(airQuality.airQualityIndex()), "air-value");
        value.setLayoutX(92);
        value.setLayoutY(64);
        value.setPrefWidth(72);
        value.setAlignment(Pos.CENTER);
        Label unit = UiText.data("IQA", "donut-unit");
        unit.setLayoutX(92);
        unit.setLayoutY(100);
        unit.setPrefWidth(72);
        unit.setAlignment(Pos.CENTER);

        Rectangle badge = new Rectangle(48, 16);
        badge.setLayoutX(104);
        badge.setLayoutY(118);
        badge.setArcWidth(16);
            badge.setArcHeight(16);
            badge.getStyleClass().add("aqi-badge");
        Label status = UiText.data(airQuality.status(), "aqi-status");
        status.setLayoutX(104);
        status.setLayoutY(119);
        status.setPrefWidth(48);
        status.setAlignment(Pos.CENTER);

        Line separator = new Line(14, 164, 242, 164);
        separator.setStroke(Palette.BORDER_SOFT);
        separator.setStrokeWidth(0.8);

        getChildren().addAll(title, base, arc, value, unit, badge, status, separator);
        pollutant("PM2.5", airQuality.pm25MicrogramsPerCubicMeter(), 181);
        pollutant("PM10", airQuality.pm10MicrogramsPerCubicMeter(), 206);
        pollutant("NO₂", airQuality.no2MicrogramsPerCubicMeter(), 231);

        AccessibilitySupport.describe(this, AccessibleRole.TEXT, "Indice de qualité de l'air " + airQuality.airQualityIndex() + ", statut " + airQuality.status(), "Carte qualité de l'air avec PM2.5, PM10 et dioxyde d'azote.");
    }

    private void pollutant(String name, int value, double y) {
        Label nameLabel = UiText.label(name, "pollutant-name");
        nameLabel.setLayoutX(20);
        nameLabel.setLayoutY(y - 10);
        Label valueLabel = UiText.data(value + " µg/m³", "pollutant-value");
        valueLabel.setLayoutX(154);
        valueLabel.setLayoutY(y - 10);
        valueLabel.setPrefWidth(82);
        getChildren().addAll(nameLabel, valueLabel);
        if (y < 230) {
            Line separator = new Line(14, y + 8, 242, y + 8);
            separator.getStyleClass().add("row-separator");
            getChildren().add(separator);
        }
    }
}
