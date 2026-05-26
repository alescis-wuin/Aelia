package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.DailyForecast;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

import java.util.List;

/**
 * Seven-day temperature chart drawn with JavaFX vector nodes for accessibility-friendly tooltips.
 */
public final class TemperatureTrendCard extends CardPane {
    private static final double[] X_POINTS = {94, 168, 242, 316, 390, 464, 538};
    private static final double CHART_TOP = 48.0;
    private static final double CHART_BOTTOM = 117.0;
    private static final int MIN_SCALE = 10;
    private static final int MAX_SCALE = 30;

    public TemperatureTrendCard(List<DailyForecast> forecasts) {
        super(556, 160);
        Label title = UiText.section("Courbe de température — 7 jours");
        title.setLayoutX(20);
        title.setLayoutY(19);
        getChildren().add(title);
        buildLegend();
        buildGrid();
        buildSeries(forecasts);
        AccessibilitySupport.describe(this, AccessibleRole.TEXT, "Courbe des températures maximales et minimales sur sept jours", "Le jaune représente les maximales et le cyan les minimales.");
    }

    private void buildLegend() {
        Line maxLine = new Line(426, 26, 446, 26);
        maxLine.setStroke(Palette.YELLOW);
        maxLine.setStrokeWidth(2.0);
        Circle maxPoint = new Circle(436, 26, 3, Palette.YELLOW);
        Label maxLabel = UiText.data("Max", "legend-max");
        maxLabel.setLayoutX(450);
        maxLabel.setLayoutY(19);

        Line minLine = new Line(482, 26, 502, 26);
        minLine.setStroke(Palette.CYAN_DARK);
        minLine.setStrokeWidth(2.0);
        Circle minPoint = new Circle(492, 26, 3, Palette.CYAN_DARK);
        Label minLabel = UiText.data("Min", "legend-min");
        minLabel.setLayoutX(506);
        minLabel.setLayoutY(19);
        getChildren().addAll(maxLine, maxPoint, maxLabel, minLine, minPoint, minLabel);
    }

    private void buildGrid() {
        int[] labels = {30, 25, 20, 15};
        for (int index = 0; index < labels.length; index++) {
            double y = 48 + index * 21.0;
            Line line = new Line(84, y, 538, y);
            line.getStyleClass().add("chart-grid-line");
            Label label = UiText.data(labels[index] + "°", "chart-axis-label");
            label.setLayoutX(30);
            label.setLayoutY(y - 8);
            getChildren().addAll(line, label);
        }
    }

    private void buildSeries(List<DailyForecast> forecasts) {
        Polyline maxLine = new Polyline();
        Polyline minLine = new Polyline();
        for (int index = 0; index < forecasts.size(); index++) {
            DailyForecast forecast = forecasts.get(index);
            double x = X_POINTS[index];
            double maxY = scale(forecast.maximumTemperatureCelsius());
            double minY = scale(forecast.minimumTemperatureCelsius());
            maxLine.getPoints().addAll(x, maxY);
            minLine.getPoints().addAll(x, minY);
            addForecastLabels(forecast, x, maxY, minY, index);
        }
        maxLine.setStroke(Palette.YELLOW);
        maxLine.setStrokeWidth(2.2);
        maxLine.setFill(null);
        maxLine.setStrokeLineCap(StrokeLineCap.ROUND);
        minLine.setStroke(Palette.CYAN_DARK);
        minLine.setStrokeWidth(2.0);
        minLine.setFill(null);
        minLine.setStrokeLineCap(StrokeLineCap.ROUND);
        getChildren().add(7, maxLine);
        getChildren().add(8, minLine);
    }

    private void addForecastLabels(DailyForecast forecast, double x, double maxY, double minY, int index) {
        Circle maxPoint = new Circle(x, maxY, 4.0, Palette.YELLOW);
        Circle minPoint = new Circle(x, minY, 3.5, Palette.CYAN_DARK);
        Label maxLabel = UiText.data(forecast.maximumTemperatureCelsius() + "°", "chart-max-label");
        maxLabel.setPrefWidth(44);
        maxLabel.setLayoutX(x - 22);
        maxLabel.setLayoutY(maxY - 23);
        Label minLabel = UiText.data(forecast.minimumTemperatureCelsius() + "°", "chart-min-label");
        minLabel.setPrefWidth(44);
        minLabel.setLayoutX(x - 22);
        minLabel.setLayoutY(minY + 3);
        Label day = UiText.data(forecast.dayLabel().substring(0, 3), "chart-day-label");
        day.setPrefWidth(44);
        day.setLayoutX(x - 22);
        day.setLayoutY(134);
        getChildren().addAll(maxPoint, minPoint, maxLabel, minLabel, day);
    }

    private double scale(int temperature) {
        double normalized = (temperature - MIN_SCALE) / (double) (MAX_SCALE - MIN_SCALE);
        return CHART_BOTTOM - normalized * (CHART_BOTTOM - CHART_TOP);
    }
}
