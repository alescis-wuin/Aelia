package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.CurrentWeather;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.Locale;

/**
 * Main current-weather card, designed as the dominant visual anchor.
 */
public final class CurrentWeatherHero extends CardPane {
    private static final double CARD_WIDTH = 556.0;
    private static final double CARD_HEIGHT = 200.0;
    private static final double DATE_PILL_WIDTH = 260.0;
    private static final double SUN_CENTER_X = 480.0;
    private static final double SUN_CENTER_Y = 108.0;
    private static final double TEMPERATURE_BLOCK_X = 16.0;
    private static final double TEMPERATURE_BLOCK_Y = 51.0;

    public CurrentWeatherHero(CurrentWeather weather) {
        super(CARD_WIDTH, CARD_HEIGHT);
        getStyleClass().add("hero-card");

        VBox temperatureBlock = buildTemperatureBlock(weather);
        temperatureBlock.setLayoutX(TEMPERATURE_BLOCK_X);
        temperatureBlock.setLayoutY(TEMPERATURE_BLOCK_Y);

        Label details = UiText.label(
                "Ressenti " + weather.apparentTemperatureCelsius() + "°     Lever " + weather.sunrise() + "     Coucher " + weather.sunset(),
                "hero-details"
        );
        details.setLayoutX(20);
        details.setLayoutY(181);

        double dateX = (CARD_WIDTH - DATE_PILL_WIDTH) / 2.0;
        Rectangle datePill = new Rectangle(DATE_PILL_WIDTH, 26);
        datePill.setLayoutX(dateX);
        datePill.setLayoutY(7);
        datePill.setArcWidth(26);
        datePill.setArcHeight(26);
        datePill.getStyleClass().add("date-pill");

        Label date = UiText.label(weather.city() + " · " + weather.dateLabel(), "date-label");
        date.setPrefWidth(DATE_PILL_WIDTH);
        date.setAlignment(Pos.CENTER);
        date.setLayoutX(dateX);
        date.setLayoutY(11);

        Node sun = WeatherIcons.arcSun();
        sun.setLayoutX(SUN_CENTER_X);
        sun.setLayoutY(SUN_CENTER_Y);

        Label condition = UiText.section(weather.conditionLabel());
        condition.getStyleClass().add("hero-condition");
        condition.setPrefWidth(176);
        condition.setAlignment(Pos.CENTER);
        condition.setLayoutX(SUN_CENTER_X - 88);
        condition.setLayoutY(166);

        getChildren().addAll(temperatureBlock, details, datePill, date, sun, condition);
        AccessibilitySupport.describe(
                this,
                AccessibleRole.TEXT,
                String.format(Locale.ROOT, "%s à %s, %d degrés, maximum %d, minimum %d", weather.conditionLabel(), weather.city(), weather.temperatureCelsius(), weather.maximumTemperatureCelsius(), weather.minimumTemperatureCelsius()),
                "Carte météo principale pour la zone sélectionnée."
        );
    }

    private VBox buildTemperatureBlock(CurrentWeather weather) {
        Label temperature = UiText.data(weather.temperatureCelsius() + "°", "hero-temperature");
        Label maximum = UiText.data("↑ " + weather.maximumTemperatureCelsius() + "°", "temperature-high");
        Label minimum = UiText.data("↓ " + weather.minimumTemperatureCelsius() + "°", "temperature-low-muted");

        HBox extrema = new HBox(10, maximum, minimum);
        extrema.setAlignment(Pos.CENTER_LEFT);

        VBox block = new VBox(-10, temperature, extrema);
        block.setAlignment(Pos.CENTER_LEFT);
        block.setPrefWidth(220);
        block.setMouseTransparent(true);
        return block;
    }
}
