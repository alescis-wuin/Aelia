package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.DailyForecast;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Seven-day forecast table with selectable daily rows.
 */
public final class DailyForecastCard extends CardPane {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRANCE);

    private final List<ForecastRowView> rowViews = new ArrayList<>();
    private int selectedIndex = -1;

    public DailyForecastCard(List<DailyForecast> forecasts) {
        super(556, 332);
        Label title = UiText.section("Prévisions 7 jours");
        title.setLayoutX(18);
        title.setLayoutY(16);
        getChildren().add(title);
        addHeaders();
        for (int index = 0; index < Math.min(7, forecasts.size()); index++) {
            DailyForecast forecast = forecasts.get(index);
            Pane row = row(forecast, index);
            row.setLayoutX(8);
            row.setLayoutY(54 + index * 38.0);
            getChildren().add(row);
            if (forecast.selected()) {
                selectedIndex = index;
            }
        }
        if (selectedIndex < 0 && !rowViews.isEmpty()) {
            selectedIndex = 0;
        }
        updateSelection();
        TooltipSupport.install(this, "Prévisions sur 7 jours. Cliquez sur une ligne pour sélectionner une journée.");
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, "Prévisions sur sept jours", "Tableau des prévisions quotidiennes.");
    }

    private void addHeaders() {
        String[] headers = {"Jour", "Météo", "Max", "Min", "Pluie", "Vent", "UV"};
        double[] x = {18, 152, 276, 334, 398, 462, 526};
        for (int i = 0; i < headers.length; i++) {
            Label header = UiText.section(headers[i]);
            header.setLayoutX(x[i]);
            header.setLayoutY(40);
            getChildren().add(header);
        }
    }

    private Pane row(DailyForecast forecast, int index) {
        Pane row = new Pane();
        row.setPrefSize(540, 34);
        row.setFocusTraversable(true);
        row.setAccessibleRole(AccessibleRole.BUTTON);
        row.setAccessibleText(forecast.dayLabel() + ", " + forecast.condition().label() + ", maximum "
                + forecast.maximumTemperatureCelsius() + ", minimum " + forecast.minimumTemperatureCelsius());
        row.setAccessibleHelp("Sélectionne cette journée.");

        Label day = UiText.label(forecast.dayLabel(), "forecast-day");
        day.setLayoutX(16);
        day.setLayoutY(8);

        Node icon = WeatherIcons.conditionIcon(forecast.condition(), 28);
        icon.setLayoutX(144);
        icon.setLayoutY(3);
        icon.setMouseTransparent(true);

        addValue(row, forecast.maximumTemperatureCelsius() + "°", "forecast-max", 246, 8, 42);
        addValue(row, forecast.minimumTemperatureCelsius() + "°", "forecast-min", 306, 8, 42);
        addValue(row, forecast.rainProbabilityPercent() + "%", forecast.rainProbabilityPercent() >= 70 ? "forecast-rain-strong" : "forecast-muted", 370, 8, 48);
        addValue(row, forecast.windSpeedKmh() + " km/h", "forecast-muted", 428, 8, 70);
        addValue(row, String.valueOf(forecast.uvIndex()), forecast.uvIndex() >= 7 ? "forecast-uv-high" : "forecast-uv", 508, 8, 28);

        Line separator = new Line(8, 34, 532, 34);
        separator.getStyleClass().add("row-separator");
        separator.setMouseTransparent(true);
        row.getChildren().addAll(day, icon, separator);
        row.setOnMouseClicked(event -> {
            if (!event.isStillSincePress()) {
                return;
            }
            select(index);
            event.consume();
        });
        row.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                select(index);
                event.consume();
            }
        });
        TooltipSupport.install(row, tooltipText(forecast));
        rowViews.add(new ForecastRowView(row, day));
        return row;
    }

    private String tooltipText(DailyForecast forecast) {
        return DATE_FORMATTER.format(forecast.date())
                + " · " + forecast.condition().label()
                + " · max " + forecast.maximumTemperatureCelsius() + " °C"
                + " · min " + forecast.minimumTemperatureCelsius() + " °C"
                + " · pluie " + forecast.rainProbabilityPercent() + " %"
                + " · vent " + forecast.windSpeedKmh() + " km/h"
                + " · UV " + forecast.uvIndex();
    }

    private void select(int index) {
        if (index < 0 || index >= rowViews.size()) {
            return;
        }
        selectedIndex = index;
        updateSelection();
    }

    private void updateSelection() {
        for (int index = 0; index < rowViews.size(); index++) {
            ForecastRowView view = rowViews.get(index);
            boolean selected = index == selectedIndex;
            view.row().getStyleClass().removeAll("forecast-row-active", "forecast-row");
            view.row().getStyleClass().add(selected ? "forecast-row-active" : "forecast-row");
            view.dayLabel().getStyleClass().removeAll("forecast-day-active", "forecast-day");
            view.dayLabel().getStyleClass().add(selected ? "forecast-day-active" : "forecast-day");
        }
    }

    private void addValue(Pane row, String text, String style, double x, double y, double width) {
        Label label = UiText.data(text, style);
        label.setLayoutX(x);
        label.setLayoutY(y);
        label.setPrefWidth(width);
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setMouseTransparent(true);
        row.getChildren().add(label);
    }

    private record ForecastRowView(Pane row, Label dayLabel) {
    }
}
