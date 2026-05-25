package fr.seynax.aelia;

import atlantafx.base.theme.PrimerDark;
import fr.seynax.aelia.controller.DashboardController;
import fr.seynax.aelia.provider.simulation.SimulatedWeatherDataProvider;
import fr.seynax.aelia.service.WeatherService;
import fr.seynax.aelia.ui.WeatherDashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * JavaFX entry point for the weather utility application.
 */
public final class WeatherUtilityApplication extends Application {

    private WeatherService weatherService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        weatherService = new WeatherService(new SimulatedWeatherDataProvider());
        WeatherDashboardView view = new WeatherDashboardView();
        DashboardController controller = new DashboardController(weatherService, view);
        controller.initialize();

        Scene scene = new Scene(view.root(), 1280, 820);
        URL stylesheet = WeatherUtilityApplication.class.getResource(
                "/fr/seynax/aelia/styles/application.css"
        );
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Aelia");
        stage.setMinWidth(1080);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> weatherService.close());
        stage.show();
    }

    @Override
    public void stop() {
        if (weatherService != null) {
            weatherService.close();
        }
    }
}
