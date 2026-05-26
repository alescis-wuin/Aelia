package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.HourlyForecast;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal strip of compact hourly forecast chips with real horizontal scrolling.
 */
public final class HourlyForecastCard extends CardPane {
    private final List<Pane> chips = new ArrayList<>();
    private int selectedIndex;

    public HourlyForecastCard(List<HourlyForecast> forecasts) {
        super(556, 94);
        Label title = UiText.section("Prévisions horaires");
        title.setLayoutX(20);
        title.setLayoutY(17);
        getChildren().add(title);

        HBox strip = new HBox(10);
        strip.setPadding(new Insets(0, 12, 0, 0));
        strip.setPrefHeight(58);
        for (int index = 0; index < forecasts.size(); index++) {
            HourlyForecast forecast = forecasts.get(index);
            Pane chip = buildChip(forecast, index);
            chips.add(chip);
            strip.getChildren().add(chip);
            if (forecast.selected()) {
                selectedIndex = index;
            }
        }

        ScrollPane scroller = new ScrollPane(strip);
        scroller.getStyleClass().add("hourly-scroll-pane");
        scroller.setLayoutX(10);
        scroller.setLayoutY(28);
        scroller.setPrefSize(536, 58);
        scroller.setMinSize(536, 58);
        scroller.setMaxSize(536, 58);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setFitToHeight(true);
        scroller.setPannable(true);
        scroller.setFocusTraversable(false);
        scroller.addEventFilter(ScrollEvent.SCROLL, event -> {
            double delta = Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY()) ? event.getDeltaX() : event.getDeltaY();
            scroller.setHvalue(Math.max(0.0, Math.min(1.0, scroller.getHvalue() - delta / 420.0)));
            event.consume();
        });
        getChildren().add(scroller);

        refreshSelection();
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, "Prévisions horaires", "Faire défiler horizontalement pour consulter les heures suivantes.");
    }

    private Pane buildChip(HourlyForecast forecast, int index) {
        Pane chip = new Pane();
        chip.setPrefSize(60, 56);
        chip.setMinSize(60, 56);
        chip.setMaxSize(60, 56);
        chip.setFocusTraversable(true);
        chip.setAccessibleRole(AccessibleRole.BUTTON);
        chip.setAccessibleText(forecast.hour() + ", " + forecast.condition().label() + ", " + forecast.temperatureCelsius() + " degrés");
        chip.setAccessibleHelp("Prévision horaire sélectionnable.");

        Label hour = UiText.data(forecast.hour(), "hour-label");
        hour.setPrefWidth(60);
        hour.setLayoutX(0);
        hour.setLayoutY(2);

        Node icon = WeatherIcons.smallCondition(forecast.condition());
        icon.setLayoutX(30);
        icon.setLayoutY(28);

        Label temperature = UiText.data(forecast.temperatureCelsius() + "°", "hour-temperature");
        temperature.setPrefWidth(60);
        temperature.setLayoutX(0);
        temperature.setLayoutY(39);

        chip.getChildren().addAll(hour, icon, temperature);
        chip.setOnMouseClicked(event -> select(index));
        chip.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                select(index);
                event.consume();
            }
        });
        return chip;
    }

    private void select(int index) {
        selectedIndex = index;
        refreshSelection();
    }

    private void refreshSelection() {
        for (int i = 0; i < chips.size(); i++) {
            chips.get(i).getStyleClass().removeAll("hour-chip", "hour-chip-active");
            chips.get(i).getStyleClass().add(i == selectedIndex ? "hour-chip-active" : "hour-chip");
        }
    }
}
