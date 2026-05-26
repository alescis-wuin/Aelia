package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.CurrentWeather;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.ui.components.AirQualityCard;
import fr.alescis.aelia.ui.components.CardPane;
import fr.alescis.aelia.ui.components.CurrentWeatherHero;
import fr.alescis.aelia.ui.components.DailyForecastCard;
import fr.alescis.aelia.ui.components.DonutGaugeCard;
import fr.alescis.aelia.ui.components.HourlyForecastCard;
import fr.alescis.aelia.ui.components.PollenCard;
import fr.alescis.aelia.ui.components.PressureCard;
import fr.alescis.aelia.ui.components.SidebarView;
import fr.alescis.aelia.ui.components.SunPathCard;
import fr.alescis.aelia.ui.components.TemperatureTrendCard;
import fr.alescis.aelia.ui.components.UvIndexCard;
import fr.alescis.aelia.ui.components.WeatherIcons;
import fr.alescis.aelia.ui.components.WindCard;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;

import java.time.LocalTime;
import java.util.Locale;

/**
 * Full desktop dashboard assembled according to the supplied 1374 x 854 mockup.
 */
public class AeliaDashboardView extends Pane {
    public static final double DESIGN_WIDTH = 1374.0;
    public static final double DESIGN_HEIGHT = 854.0;

    private final DashboardSnapshot snapshot;
    private final Pane mainLayer = new Pane();
    private final Pane overlayLayer = new Pane();
    private int selectedLocationIndex;

    @SuppressWarnings("this-escape")
    public AeliaDashboardView(DashboardSnapshot snapshot) {
        this.snapshot = snapshot;
        this.selectedLocationIndex = selectedLocationIndex(snapshot);
        setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        getStyleClass().add("dashboard-root");
        setAccessibleRole(AccessibleRole.PARENT);
        setAccessibleText("Aelia weather dashboard for Paris");
        buildShell();
    }

    private int selectedLocationIndex(DashboardSnapshot snapshot) {
        for (int index = 0; index < snapshot.locations().size(); index++) {
            if (snapshot.locations().get(index).selected()) {
                return index;
            }
        }
        return 0;
    }

    private void buildShell() {
        mainLayer.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        overlayLayer.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        overlayLayer.setVisible(false);
        overlayLayer.setManaged(false);
        getChildren().addAll(mainLayer, overlayLayer);
        SidebarView sidebar = new SidebarView(snapshot.locations(), this::selectLocation, this::selectNavigationView);
        placeOnRoot(sidebar, 0, 0);
        renderMainDashboard(currentWeatherForSelectedLocation());
    }

    private void renderMainDashboard(CurrentWeather currentWeather) {
        mainLayer.getChildren().clear();
        placeOnMain(new CurrentWeatherHero(currentWeather), 280, 16);
        placeOnMain(new HourlyForecastCard(snapshot.hourlyForecasts()), 280, 228);
        placeOnMain(new TemperatureTrendCard(snapshot.dailyForecasts()), 280, 334);
        placeOnMain(new DailyForecastCard(snapshot.dailyForecasts()), 280, 506);

        placeOnMain(new DonutGaugeCard(
                "Humidité",
                String.valueOf(currentWeather.humidityPercent()),
                "%",
                currentWeather.humidityPercent() / 100.0,
                Palette.CYAN,
                "Modérée",
                "Idéal 40–60%",
                WeatherIcons.droplet(),
                "Humidité relative " + currentWeather.humidityPercent() + " pour cent"
        ), 852, 16);

        placeOnMain(new WindCard(currentWeather.windSpeedKmh(), currentWeather.windDirection(), currentWeather.windGustKmh()), 1026, 16);
        placeOnMain(new PressureCard(currentWeather.pressureHpa(), currentWeather.pressureTrend()), 1200, 16);
        placeOnMain(new UvIndexCard(currentWeather.uvIndex(), currentWeather.uvAdvice()), 852, 192);
        placeOnMain(new AirQualityCard(currentWeather.airQuality()), 852, 328);
        placeOnMain(new PollenCard(snapshot.pollenRisks()), 1120, 328);
        placeOnMain(new SunPathCard(currentWeather), 852, 594);
    }

    private void selectLocation(int index) {
        if (index < 0 || index >= snapshot.locations().size()) {
            return;
        }
        selectedLocationIndex = index;
        renderMainDashboard(currentWeatherForSelectedLocation());
        mainLayer.setVisible(true);
        overlayLayer.setVisible(false);
    }

    private CurrentWeather currentWeatherForSelectedLocation() {
        LocationWeather location = snapshot.locations().get(selectedLocationIndex);
        CurrentWeather base = snapshot.currentWeather();
        if (selectedLocationIndex == selectedLocationIndex(snapshot)) {
            return base;
        }
        int temperature = location.temperatureCelsius();
        int maximumOffset = Math.max(3, base.maximumTemperatureCelsius() - base.temperatureCelsius());
        int minimumOffset = Math.max(4, base.temperatureCelsius() - base.minimumTemperatureCelsius());
        int apparentOffset = base.temperatureCelsius() - base.apparentTemperatureCelsius();
        return new CurrentWeather(
                location.city(),
                location.condition().label(),
                base.date(),
                base.zoneId(),
                temperature,
                temperature + maximumOffset,
                temperature - minimumOffset,
                temperature - apparentOffset,
                base.sunriseTime(),
                base.sunsetTime(),
                base.humidityPercent(),
                base.windSpeedKmh(),
                base.windDirection(),
                base.windGustKmh(),
                base.pressureHpa(),
                base.pressureTrend(),
                base.uvIndex(),
                base.uvAdvice(),
                base.airQuality(),
                clampSolarTime(base.currentSolarLocalTime())
        );
    }

    private LocalTime clampSolarTime(LocalTime time) {
        return time == null ? LocalTime.NOON : time;
    }

    private void selectNavigationView(String id) {
        if ("home".equals(id)) {
            mainLayer.setVisible(true);
            overlayLayer.setVisible(false);
            return;
        }
        mainLayer.setVisible(false);
        overlayLayer.getChildren().clear();
        overlayLayer.setVisible(true);
        String title = switch (id) {
            case "map" -> "Carte";
            case "settings" -> "Réglages";
            default -> id == null ? "Vue" : id;
        };
        overlayLayer.getChildren().add(inProgressView(title));
    }

    private Pane inProgressView(String title) {
        CardPane card = new CardPane(1094, 822);
        card.getStyleClass().add("work-in-progress-card");
        card.setLayoutX(280);
        card.setLayoutY(16);

        Label heading = UiText.brand(title.toUpperCase(Locale.FRANCE));
        heading.getStyleClass().add("work-in-progress-title");
        heading.setAlignment(Pos.CENTER);
        heading.setPrefWidth(1094);
        heading.setLayoutX(0);
        heading.setLayoutY(340);

        Label body = UiText.label("En cours", "work-in-progress-body");
        body.setAlignment(Pos.CENTER);
        body.setPrefWidth(1094);
        body.setLayoutX(0);
        body.setLayoutY(380);

        Label hint = UiText.label("Cette vue est connectée à la navigation et sera enrichie sans modifier le tableau de bord principal.", "work-in-progress-hint");
        hint.setAlignment(Pos.CENTER);
        hint.setPrefWidth(1094);
        hint.setLayoutX(0);
        hint.setLayoutY(412);

        card.getChildren().addAll(heading, body, hint);
        return card;
    }

    private void placeOnRoot(Pane pane, double x, double y) {
        pane.setLayoutX(x);
        pane.setLayoutY(y);
        getChildren().add(pane);
    }

    private void placeOnMain(Pane pane, double x, double y) {
        pane.setLayoutX(x);
        pane.setLayoutY(y);
        mainLayer.getChildren().add(pane);
    }
}
