package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.DailyForecast;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Seven-day forecast table with custom selectable rows.
 */
public final class DailyForecastCard extends CardPane {
    private final List<Pane> rows = new ArrayList<>();
    private int selectedIndex;

    public DailyForecastCard(List<DailyForecast> forecasts) {
        super(556, 332);
        Label title = UiText.section("Prévisions 7 jours");
        title.setLayoutX(20);
        title.setLayoutY(19);
        getChildren().add(title);
        buildHeader();
        for (int index = 0; index < forecasts.size(); index++) {
            DailyForecast forecast = forecasts.get(index);
            Pane row = buildRow(forecast, index);
            row.setLayoutX(6);
            row.setLayoutY(51 + index * 38.0);
            rows.add(row);
            getChildren().add(row);
            if (forecast.selected()) {
                selectedIndex = index;
            }
        }
        refreshSelection();
        AccessibilitySupport.describe(this, AccessibleRole.TABLE_VIEW, "Tableau des prévisions sur sept jours", "Chaque ligne peut être sélectionnée au clavier ou à la souris.");
    }

    private void buildHeader() {
        String[] labels = {"JOUR", "MÉTÉO", "MAX", "MIN", "PLUIE", "VENT", "UV"};
        double[] x = {20, 148, 236, 296, 356, 436, 526};
        for (int index = 0; index < labels.length; index++) {
            Label label = UiText.section(labels[index]);
            label.setLayoutX(x[index]);
            label.setLayoutY(39);
            if (index > 0) {
                label.setAlignment(Pos.CENTER);
                label.setPrefWidth(index == 6 ? 18 : 60);
            }
            getChildren().add(label);
        }
        Line separator = new Line(10, 47, 546, 47);
        separator.setStroke(Palette.BORDER_SOFT);
        separator.setStrokeWidth(1.0);
        getChildren().add(separator);
    }

    private Pane buildRow(DailyForecast forecast, int index) {
        Pane row = new Pane();
        row.setPrefSize(544, 38);
        row.setFocusTraversable(true);
        row.setAccessibleRole(AccessibleRole.LIST_ITEM);
        row.setAccessibleText(forecast.dayLabel() + ", maximum " + forecast.maximumTemperatureCelsius() + ", minimum " + forecast.minimumTemperatureCelsius() + ", pluie " + forecast.rainProbabilityPercent() + " pour cent, vent " + forecast.windSpeedKmh() + " kilomètres par heure, UV " + forecast.uvIndex());
        row.setAccessibleHelp("Ligne de prévision quotidienne.");

        Label day = UiText.label(forecast.dayLabel(), index == 0 ? "forecast-day-active" : "forecast-day");
        day.setLayoutX(14);
        day.setLayoutY(10);

        Node icon = WeatherIcons.smallCondition(forecast.condition());
        icon.setLayoutX(142);
        icon.setLayoutY(19);

        Label max = centeredData(forecast.maximumTemperatureCelsius() + "°", "forecast-max", 207, 10, 60);
        Label min = centeredData(forecast.minimumTemperatureCelsius() + "°", "forecast-min", 267, 10, 60);
        Label rain = centeredData(forecast.rainProbabilityPercent() + "%", forecast.rainProbabilityPercent() >= 60 ? "forecast-rain-strong" : "forecast-muted", 327, 10, 60);
        Label wind = centeredData(forecast.windSpeedKmh() + " km/h", "forecast-muted", 392, 10, 90);
        Label uv = centeredData(String.valueOf(forecast.uvIndex()), forecast.uvIndex() >= 7 ? "forecast-uv-high" : "forecast-uv", 503, 10, 26);

        row.getChildren().addAll(day, icon, max, min, rain, wind, uv);
        row.setOnMouseClicked(event -> select(index));
        row.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                select(index);
                event.consume();
            }
        });

        Line separator = new Line(4, 38, 540, 38);
        separator.getStyleClass().add("row-separator");
        row.getChildren().add(separator);
        return row;
    }

    private Label centeredData(String text, String styleClass, double x, double y, double width) {
        Label label = UiText.data(text, styleClass);
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setPrefWidth(width);
        label.setAlignment(Pos.CENTER);
        return label;
    }

    private void select(int index) {
        selectedIndex = index;
        refreshSelection();
    }

    private void refreshSelection() {
        for (int i = 0; i < rows.size(); i++) {
            Pane row = rows.get(i);
            row.getStyleClass().removeAll("forecast-row", "forecast-row-active");
            row.getStyleClass().add(i == selectedIndex ? "forecast-row-active" : "forecast-row");
        }
    }
}
