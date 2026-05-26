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
import javafx.scene.shape.StrokeLineCap;

/**
 * Atmospheric pressure card with a semi-circular gauge.
 */
public final class PressureCard extends CardPane {
    private static final double MIN_PRESSURE_HPA = 980.0;
    private static final double MAX_PRESSURE_HPA = 1040.0;

    public PressureCard(int pressureHpa, String trend) {
        super(174, 164);
        Label title = UiText.section("Pression atm.");
        title.setLayoutX(18);
        title.setLayoutY(16);

        double centerX = 87.0;
        double centerY = 84.0;
        double radius = 42.0;
        Arc base = new Arc(centerX, centerY, radius, radius, 180, -180);
        base.setFill(null);
        base.setStroke(Palette.BORDER_SOFT);
        base.setStrokeWidth(8.0);
        base.setType(ArcType.OPEN);
        base.setStrokeLineCap(StrokeLineCap.ROUND);

        double progress = GaugeMath.normalize(pressureHpa, MIN_PRESSURE_HPA, MAX_PRESSURE_HPA);
        Arc arc = new Arc(centerX, centerY, radius, radius, 180, -180.0 * progress);
        arc.setFill(null);
        arc.setStroke(Palette.CYAN);
        arc.setStrokeWidth(8.0);
        arc.setType(ArcType.OPEN);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);

        GaugePoint dotPosition = GaugeMath.semicirclePoint(centerX, centerY, radius, radius, progress);
        Circle dot = new Circle(dotPosition.x(), dotPosition.y(), 4, Palette.TEXT);

        Label min = UiText.data("980", "pressure-scale");
        min.setLayoutX(32);
        min.setLayoutY(96);
        Label max = UiText.data("1040", "pressure-scale");
        max.setLayoutX(130);
        max.setLayoutY(96);

        Label value = UiText.data(String.valueOf(pressureHpa), "pressure-value");
        Label unit = UiText.data("hPa", "donut-unit");
        VBox centerText = centeredValueBlock(value, unit, 87, 91, 82, 56);

        Label trendLabel = UiText.label(trend, "pressure-trend");
        trendLabel.setLayoutX(18);
        trendLabel.setLayoutY(132);
        trendLabel.setPrefWidth(74);

        Label normal = UiText.label("Normal 1013", "metric-hint");
        normal.setLayoutX(86);
        normal.setLayoutY(132);
        normal.setPrefWidth(78);
        normal.setAlignment(Pos.CENTER_RIGHT);

        getChildren().addAll(title, base, arc, dot, min, max, centerText, trendLabel, normal);
        String tooltipText = "Pression atmosphérique " + pressureHpa + " hPa · " + trend + " · normale 1013 hPa";
        TooltipSupport.install(this, tooltipText);
        AccessibilitySupport.describe(this, AccessibleRole.TEXT,
                "Pression atmosphérique " + pressureHpa + " hectopascals, " + trend,
                "Carte de pression atmosphérique.");
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
