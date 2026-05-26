package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.DailyForecast;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Seven-day min/max temperature trend chart with point hover crosshairs.
 */
public final class TemperatureTrendCard extends CardPane {
    private static final double CHART_LEFT = 80.0;
    private static final double CHART_TOP = 46.0;
    private static final double CHART_WIDTH = 458.0;
    private static final double CHART_HEIGHT = 84.0;
    private static final int MIN_AXIS = 10;
    private static final int MAX_AXIS = 32;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE);

    private final Line verticalCrosshair = new Line();
    private final Line horizontalCrosshair = new Line();

    public TemperatureTrendCard(List<DailyForecast> forecasts) {
        super(556, 160);
        Label title = UiText.section("Courbe de température — 7 jours");
        title.setLayoutX(18);
        title.setLayoutY(16);
        getChildren().add(title);
        drawGrid();
        configureCrosshairs();
        drawSeries(forecasts, true);
        drawSeries(forecasts, false);
        drawLegend();
        TooltipSupport.install(this, "Courbe des températures maximales et minimales sur 7 jours.");
        AccessibilitySupport.describe(this, AccessibleRole.TEXT, "Courbe de température sur sept jours", "Affiche les températures maximales et minimales.");
    }

    private void drawGrid() {
        int[] labels = {30, 25, 20, 15};
        for (int label : labels) {
            double y = yFor(label);
            Line line = new Line(CHART_LEFT, y, CHART_LEFT + CHART_WIDTH, y);
            line.getStyleClass().add("chart-grid-line");
            Label axis = UiText.data(label + "°", "chart-axis-label");
            axis.setLayoutX(28);
            axis.setLayoutY(y - 8);
            getChildren().addAll(line, axis);
        }
        Rectangle fill = new Rectangle(CHART_LEFT, yFor(25), CHART_WIDTH, yFor(10) - yFor(25));
        fill.setFill(Palette.withOpacity(Palette.YELLOW, 0.035));
        fill.setMouseTransparent(true);
        getChildren().add(fill);
    }

    private void configureCrosshairs() {
        configureCrosshair(verticalCrosshair);
        configureCrosshair(horizontalCrosshair);
        getChildren().addAll(verticalCrosshair, horizontalCrosshair);
    }

    private void configureCrosshair(Line line) {
        line.getStyleClass().add("chart-crosshair-line");
        line.setMouseTransparent(true);
        line.setVisible(false);
        line.setManaged(false);
    }

    private void drawSeries(List<DailyForecast> forecasts, boolean maximum) {
        Polyline line = new Polyline();
        line.setStroke(maximum ? Palette.YELLOW : Palette.CYAN_DARK);
        line.setStrokeWidth(2.4);
        line.setFill(null);
        line.setMouseTransparent(true);
        int count = Math.min(7, forecasts.size());
        for (int i = 0; i < count; i++) {
            DailyForecast forecast = forecasts.get(i);
            int value = maximum ? forecast.maximumTemperatureCelsius() : forecast.minimumTemperatureCelsius();
            double x = xFor(i, count);
            double y = yFor(value);
            line.getPoints().addAll(x, y);
        }
        getChildren().add(line);
        for (int i = 0; i < count; i++) {
            DailyForecast forecast = forecasts.get(i);
            int value = maximum ? forecast.maximumTemperatureCelsius() : forecast.minimumTemperatureCelsius();
            double x = xFor(i, count);
            double y = yFor(value);
            Circle point = new Circle(x, y, 4, maximum ? Palette.YELLOW : Palette.CYAN_DARK);
            installPointHover(point, forecast, value, x, y, maximum);
            Label valueLabel = UiText.data(value + "°", maximum ? "chart-max-label" : "chart-min-label");
            valueLabel.setLayoutX(x - 14);
            valueLabel.setLayoutY(y + (maximum ? -22 : 8));
            valueLabel.setPrefWidth(28);
            valueLabel.setMouseTransparent(true);
            getChildren().addAll(point, valueLabel);
            if (maximum) {
                Label day = UiText.label(dayShort(forecast.dayLabel()), "chart-day-label");
                day.setLayoutX(x - 14);
                day.setLayoutY(134);
                day.setPrefWidth(28);
                day.setMouseTransparent(true);
                getChildren().add(day);
            }
        }
    }

    private void installPointHover(Circle point, DailyForecast forecast, int value, double x, double y, boolean maximum) {
        point.setOnMouseEntered(event -> showCrosshairs(x, y));
        point.setOnMouseExited(event -> hideCrosshairs());
        TooltipSupport.install(point,
                DATE_FORMATTER.format(forecast.date())
                        + " · " + (maximum ? "maximum" : "minimum")
                        + " " + value + " °C");
    }

    private void showCrosshairs(double x, double y) {
        verticalCrosshair.setStartX(x);
        verticalCrosshair.setEndX(x);
        verticalCrosshair.setStartY(CHART_TOP);
        verticalCrosshair.setEndY(CHART_TOP + CHART_HEIGHT);
        horizontalCrosshair.setStartX(CHART_LEFT);
        horizontalCrosshair.setEndX(CHART_LEFT + CHART_WIDTH);
        horizontalCrosshair.setStartY(y);
        horizontalCrosshair.setEndY(y);
        verticalCrosshair.setVisible(true);
        horizontalCrosshair.setVisible(true);
        verticalCrosshair.toFront();
        horizontalCrosshair.toFront();
    }

    private void hideCrosshairs() {
        verticalCrosshair.setVisible(false);
        horizontalCrosshair.setVisible(false);
    }

    private void drawLegend() {
        Line maxLine = new Line(430, 24, 454, 24);
        maxLine.setStroke(Palette.YELLOW);
        maxLine.setStrokeWidth(2.0);
        Circle maxPoint = new Circle(442, 24, 3, Palette.YELLOW);
        Label maxLabel = UiText.label("Max", "legend-max");
        maxLabel.setLayoutX(459);
        maxLabel.setLayoutY(17);

        Line minLine = new Line(488, 24, 512, 24);
        minLine.setStroke(Palette.CYAN_DARK);
        minLine.setStrokeWidth(2.0);
        Circle minPoint = new Circle(500, 24, 3, Palette.CYAN_DARK);
        Label minLabel = UiText.label("Min", "legend-min");
        minLabel.setLayoutX(517);
        minLabel.setLayoutY(17);
        TooltipSupport.install(maxPoint, "Températures maximales");
        TooltipSupport.install(minPoint, "Températures minimales");
        getChildren().addAll(maxLine, maxPoint, maxLabel, minLine, minPoint, minLabel);
    }

    private double xFor(int index, int count) {
        return count == 1 ? CHART_LEFT : CHART_LEFT + index * (CHART_WIDTH / (count - 1.0));
    }

    private double yFor(int value) {
        double normalized = GaugeMath.normalize(value, MIN_AXIS, MAX_AXIS);
        return CHART_TOP + CHART_HEIGHT - normalized * CHART_HEIGHT;
    }

    private String dayShort(String label) {
        int separator = label.indexOf(' ');
        return separator > 0 ? label.substring(0, separator) : label;
    }
}
