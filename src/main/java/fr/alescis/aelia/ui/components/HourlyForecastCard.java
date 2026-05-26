package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.HourlyForecast;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.UiFormatters;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.List;

/**
 * Compact horizontal hourly forecast card with selectable and scrollable chips.
 */
public final class HourlyForecastCard extends CardPane {
    private static final double CARD_WIDTH = 556.0;
    private static final double CARD_HEIGHT = 94.0;
    private static final double CHIP_WIDTH = 60.0;
    private static final double CHIP_HEIGHT = 56.0;
    private static final double CHIP_SPACING = 65.0;
    private static final double CONTENT_LEFT_PADDING = 3.0;
    private static final double CONTENT_RIGHT_PADDING = 12.0;

    private final List<HourChipView> chipViews = new ArrayList<>();
    private int selectedIndex = -1;

    public HourlyForecastCard(List<HourlyForecast> forecasts) {
        super(CARD_WIDTH, CARD_HEIGHT);
        Label title = UiText.section("Prévisions horaires");
        title.setLayoutX(18);
        title.setLayoutY(16);
        getChildren().add(title);

        ScrollPane scrollPane = buildScrollPane(forecasts);
        scrollPane.setLayoutX(20);
        scrollPane.setLayoutY(29);
        getChildren().add(scrollPane);
        TooltipSupport.install(this, "Prévisions horaires. Utilisez la molette, le glisser-déposer ou le clavier pour parcourir les heures.");
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, "Prévisions horaires", "Liste des prévisions météo heure par heure.");
    }

    private ScrollPane buildScrollPane(List<HourlyForecast> forecasts) {
        Pane content = new Pane();
        int count = forecasts.size();
        double contentWidth = CONTENT_LEFT_PADDING + Math.max(1, count) * CHIP_SPACING - (CHIP_SPACING - CHIP_WIDTH) + CONTENT_RIGHT_PADDING;
        content.setPrefSize(Math.max(516.0, contentWidth), CHIP_HEIGHT + 4.0);

        for (int index = 0; index < count; index++) {
            HourlyForecast forecast = forecasts.get(index);
            Pane chip = hourChip(forecast, index);
            chip.setLayoutX(CONTENT_LEFT_PADDING + index * CHIP_SPACING);
            chip.setLayoutY(0);
            content.getChildren().add(chip);
            HourChipView view = new HourChipView(chip, forecast);
            chipViews.add(view);
            if (forecast.selected()) {
                selectedIndex = index;
            }
        }
        if (selectedIndex < 0 && !chipViews.isEmpty()) {
            selectedIndex = 0;
        }
        updateSelection();

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.getStyleClass().add("hourly-scroll-pane");
        scrollPane.setPrefSize(516, 60);
        scrollPane.setMinSize(516, 60);
        scrollPane.setMaxSize(516, 60);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setFocusTraversable(false);
        enableWheelScroll(scrollPane);
        enableDragScroll(scrollPane, content);
        return scrollPane;
    }

    private Pane hourChip(HourlyForecast forecast, int index) {
        Pane chip = new Pane();
        chip.setPrefSize(CHIP_WIDTH, CHIP_HEIGHT);
        chip.setFocusTraversable(true);
        chip.setAccessibleRole(AccessibleRole.BUTTON);
        chip.setAccessibleText(UiFormatters.hour(forecast.time()) + ", " + forecast.condition().label() + ", " + forecast.temperatureCelsius() + " degrés");
        chip.setAccessibleHelp("Sélectionne cette prévision horaire.");

        Label hour = UiText.label(UiFormatters.hour(forecast.time()), "hour-label");
        hour.setAlignment(Pos.CENTER);
        hour.setPrefWidth(CHIP_WIDTH);
        hour.setLayoutX(0);
        hour.setLayoutY(0);

        Node icon = WeatherIcons.conditionIcon(forecast.condition(), 30);
        icon.setLayoutX(15);
        icon.setLayoutY(16);
        icon.setMouseTransparent(true);

        Label temperature = UiText.data(forecast.temperatureCelsius() + "°", "hour-temperature");
        temperature.setAlignment(Pos.CENTER);
        temperature.setPrefWidth(CHIP_WIDTH);
        temperature.setLayoutX(0);
        temperature.setLayoutY(42);

        chip.getChildren().addAll(hour, icon, temperature);
        chip.setOnMouseClicked(event -> {
            if (!event.isStillSincePress()) {
                return;
            }
            select(index);
            event.consume();
        });
        chip.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                select(index);
                event.consume();
            }
        });
        TooltipSupport.install(chip, UiFormatters.hour(forecast.time()) + " · " + forecast.condition().label() + " · " + forecast.temperatureCelsius() + " °C");
        return chip;
    }

    private void select(int index) {
        if (index < 0 || index >= chipViews.size()) {
            return;
        }
        selectedIndex = index;
        updateSelection();
    }

    private void updateSelection() {
        for (int index = 0; index < chipViews.size(); index++) {
            Pane chip = chipViews.get(index).pane();
            chip.getStyleClass().removeAll("hour-chip-active", "hour-chip");
            chip.getStyleClass().add(index == selectedIndex ? "hour-chip-active" : "hour-chip");
        }
    }

    private void enableWheelScroll(ScrollPane scrollPane) {
        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double delta = Math.abs(event.getDeltaX()) > Math.abs(event.getDeltaY()) ? event.getDeltaX() : event.getDeltaY();
            if (Math.abs(delta) < 0.1) {
                return;
            }
            double direction = delta > 0.0 ? -1.0 : 1.0;
            scrollPane.setHvalue(GaugeMath.clamp(scrollPane.getHvalue() + direction * 0.08, 0.0, 1.0));
            event.consume();
        });
    }

    private void enableDragScroll(ScrollPane scrollPane, Pane content) {
        final double[] anchorX = {0.0};
        final double[] anchorHValue = {0.0};
        content.setOnMousePressed(event -> {
            anchorX[0] = event.getSceneX();
            anchorHValue[0] = scrollPane.getHvalue();
        });
        content.setOnMouseDragged(event -> {
            double overflow = Math.max(1.0, content.getBoundsInLocal().getWidth() - scrollPane.getViewportBounds().getWidth());
            double delta = anchorX[0] - event.getSceneX();
            scrollPane.setHvalue(GaugeMath.clamp(anchorHValue[0] + delta / overflow, 0.0, 1.0));
            event.consume();
        });
    }

    private record HourChipView(Pane pane, HourlyForecast forecast) {
    }
}
