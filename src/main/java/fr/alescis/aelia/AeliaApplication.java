package fr.alescis.aelia;

import atlantafx.base.theme.PrimerDark;
import fr.alescis.aelia.model.DashboardDataStatus;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.provider.ProviderDiagnostics;
import fr.alescis.aelia.provider.WeatherDashboardProvider;
import fr.alescis.aelia.provider.WeatherProviderFactory;
import fr.alescis.aelia.service.AeliaWeatherService;
import fr.alescis.aelia.ui.AeliaDashboardView;
import fr.alescis.aelia.ui.DashboardRuntimeActions;
import fr.alescis.aelia.ui.components.ScaledDashboardShell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final List<String> STYLESHEETS = List.of(
            "/fr/alescis/aelia/styles/application.css",
            "/fr/alescis/aelia/styles/provider.css"
    );

    private volatile AeliaWeatherService service;
    private volatile String providerMode;
    private AeliaDashboardView dashboardView;
    private ExecutorService runtimeExecutor;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        loadOptionalLocalFonts();

        runtimeExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "aelia-runtime-provider");
            thread.setDaemon(true);
            return thread;
        });

        providerMode = WeatherProviderFactory.providerMode();
        ProviderDiagnostics.info("Selected weather provider mode: " + providerMode + ".");
        WeatherDashboardProvider provider = WeatherProviderFactory.createDashboardProvider(providerMode);
        service = new AeliaWeatherService(provider);
        DashboardSnapshot initialSnapshot = WeatherProviderFactory.initialSnapshot(provider, providerMode);

        dashboardView = new AeliaDashboardView(initialSnapshot, new ApplicationRuntimeActions());
        ScaledDashboardShell shell = new ScaledDashboardShell(dashboardView);

        Scene scene = new Scene(shell, INITIAL_WIDTH, INITIAL_HEIGHT);
        loadStylesheets(scene);

        stage.setTitle("Aelia");
        stage.setMinWidth(1180.0);
        stage.setMinHeight(760.0);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> closeService());
        stage.show();

        if (WeatherProviderFactory.remoteRefreshEnabled(providerMode)) {
            refreshProviderData();
        }
    }

    @Override
    public void stop() {
        closeService();
    }

    private void selectProviderMode(String mode) {
        String normalizedMode = WeatherProviderFactory.normalizeMode(mode);
        System.setProperty("aelia.weather.provider", normalizedMode);
        ProviderDiagnostics.info("Switching weather provider mode to " + normalizedMode + ".");
        runtimeExecutor.execute(() -> {
            AeliaWeatherService previousService = service;
            if (previousService != null) {
                previousService.close();
            }
            WeatherDashboardProvider provider = WeatherProviderFactory.createDashboardProvider(normalizedMode);
            AeliaWeatherService nextService = new AeliaWeatherService(provider);
            DashboardSnapshot snapshot = WeatherProviderFactory.initialSnapshot(provider, normalizedMode);
            service = nextService;
            providerMode = normalizedMode;
            Platform.runLater(() -> dashboardView.updateSnapshot(snapshot));
            if (WeatherProviderFactory.remoteRefreshEnabled(normalizedMode)) {
                refreshProviderData();
            }
        });
    }

    private void refreshProviderData() {
        AeliaWeatherService activeService = service;
        String activeMode = providerMode;
        if (activeService == null || runtimeExecutor == null) {
            return;
        }
        runtimeExecutor.execute(() -> {
            try {
                DashboardSnapshot snapshot = activeService.currentSnapshot();
                Platform.runLater(() -> dashboardView.updateSnapshot(snapshot.withDataStatus(
                        snapshot.dataStatus().withProviderMode(activeMode)
                )));
            } catch (RuntimeException exception) {
                ProviderDiagnostics.warn("Remote weather refresh failed.", exception);
                DashboardSnapshot fallback = WeatherProviderFactory.simulatedSnapshot(activeMode).withDataStatus(
                        DashboardDataStatus.remoteFailureFallback(activeMode, "API météo", WeatherProviderFactory.compactFailure(exception))
                );
                Platform.runLater(() -> dashboardView.updateSnapshot(fallback));
            }
        });
    }

    private void closeService() {
        AeliaWeatherService activeService = service;
        if (activeService != null) {
            activeService.close();
            service = null;
        }
        if (runtimeExecutor != null) {
            runtimeExecutor.shutdownNow();
            runtimeExecutor = null;
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

    private void loadStylesheets(Scene scene) {
        for (String stylesheetPath : STYLESHEETS) {
            URL stylesheet = AeliaApplication.class.getResource(stylesheetPath);
            if (stylesheet != null) {
                scene.getStylesheets().add(stylesheet.toExternalForm());
            }
        }
    }

    private final class ApplicationRuntimeActions implements DashboardRuntimeActions {
        @Override
        public void selectProviderMode(String mode) {
            AeliaApplication.this.selectProviderMode(mode);
        }

        @Override
        public void refreshProviderData() {
            AeliaApplication.this.refreshProviderData();
        }
    }
}
