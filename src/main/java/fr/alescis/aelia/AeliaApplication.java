package fr.alescis.aelia;

import atlantafx.base.theme.PrimerDark;
import fr.alescis.aelia.provider.simulation.SimulatedWeatherDashboardProvider;
import fr.alescis.aelia.service.AeliaWeatherService;
import fr.alescis.aelia.ui.AeliaDashboardView;
import fr.alescis.aelia.ui.components.ScaledDashboardShell;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;

/**
 * JavaFX entry point for Aelia.
 */
public final class AeliaApplication extends Application {

    private static final double INITIAL_WIDTH = 1460.0;
    private static final double INITIAL_HEIGHT = 908.0;
    private static final List<String> LOCAL_FONTS = List.of(
            "/fr/alescis/aelia/fonts/Luciole-Regular.ttf",
            "/fr/alescis/aelia/fonts/Luciole-Bold.ttf",
            "/fr/alescis/aelia/fonts/Hack-Regular.ttf",
            "/fr/alescis/aelia/fonts/Hack-Bold.ttf"
    );

    private AeliaWeatherService service;

    /**
     * Launches the desktop application.
     *
     * @param args runtime arguments forwarded by JavaFX
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        loadOptionalLocalFonts();

        service = new AeliaWeatherService(new SimulatedWeatherDashboardProvider());
        AeliaDashboardView dashboardView = new AeliaDashboardView(service.currentSnapshot());
        ScaledDashboardShell shell = new ScaledDashboardShell(dashboardView);

        Scene scene = new Scene(shell, INITIAL_WIDTH, INITIAL_HEIGHT);
        URL stylesheet = AeliaApplication.class.getResource("/fr/alescis/aelia/styles/application.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Aelia");
        stage.setMinWidth(1180.0);
        stage.setMinHeight(760.0);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> service.close());
        stage.show();
    }

    @Override
    public void stop() {
        if (service != null) {
            service.close();
        }
    }

    private void loadOptionalLocalFonts() {
        for (String fontPath : LOCAL_FONTS) {
            URL fontResource = AeliaApplication.class.getResource(fontPath);
            if (fontResource != null) {
                Font.loadFont(fontResource.toExternalForm(), 12.0);
            }
        }
    }
}
