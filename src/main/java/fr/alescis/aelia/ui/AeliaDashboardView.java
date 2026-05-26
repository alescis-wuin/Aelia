package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.CurrentWeather;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.ui.components.AirQualityCard;
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
import javafx.scene.AccessibleRole;
import javafx.scene.layout.Pane;

/**
 * Full desktop dashboard assembled according to the supplied 1374 x 854 mockup.
 */
public final class AeliaDashboardView extends Pane {
    public static final double DESIGN_WIDTH = 1374.0;
    public static final double DESIGN_HEIGHT = 854.0;

    public AeliaDashboardView(DashboardSnapshot snapshot) {
        setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        getStyleClass().add("dashboard-root");
        setAccessibleRole(AccessibleRole.PARENT);
        setAccessibleText("Aelia weather dashboard for Paris");
        build(snapshot);
    }

    private void build(DashboardSnapshot snapshot) {
        CurrentWeather currentWeather = snapshot.currentWeather();
        SidebarView sidebar = new SidebarView(snapshot.locations());
        place(sidebar, 0, 0);

        place(new CurrentWeatherHero(currentWeather), 280, 16);
        place(new HourlyForecastCard(snapshot.hourlyForecasts()), 280, 228);
        place(new TemperatureTrendCard(snapshot.dailyForecasts()), 280, 334);
        place(new DailyForecastCard(snapshot.dailyForecasts()), 280, 506);

        place(new DonutGaugeCard(
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

        place(new WindCard(currentWeather.windSpeedKmh(), currentWeather.windDirection(), currentWeather.windGustKmh()), 1026, 16);
        place(new PressureCard(currentWeather.pressureHpa(), currentWeather.pressureTrend()), 1200, 16);
        place(new UvIndexCard(currentWeather.uvIndex(), currentWeather.uvAdvice()), 852, 192);
        place(new AirQualityCard(currentWeather.airQuality()), 852, 328);
        place(new PollenCard(snapshot.pollenRisks()), 1120, 328);
        place(new SunPathCard(currentWeather), 852, 594);
    }

    private void place(Pane pane, double x, double y) {
        pane.setLayoutX(x);
        pane.setLayoutY(y);
        getChildren().add(pane);
    }
}
