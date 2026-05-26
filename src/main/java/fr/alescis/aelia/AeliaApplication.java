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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JavaFX entry point for Aelia.
 */
public final class AeliaApplication extends Application {

    private static final double INITIAL_WIDTH = 1460.0;
    private static final double INITIAL_HEIGHT = 908.0;
    private static final Duration SUCCESS_REFRESH_DELAY = Duration.ofMinutes(15);
    private static final List<String> LOCAL_FONTS = List.of(
            "/fr/alescis/aelia/fonts/Luciole-Regular.ttf",
            "/fr/alescis/aelia/fonts/Luciole-Bold.ttf",
            "/fr/alescis/aelia/fonts/Hack-Regular.ttf",
            "/fr/alescis/aelia/fonts/Hack-Bold.ttf"
    );

    private AeliaWeatherService service;
    private ScheduledExecutorService remoteRefreshExecutor;
    private ScheduledFuture<?> scheduledRefresh;
    private Scene scene;
    private String providerMode;
    private int consecutiveRefreshFailures;
    private volatile boolean stopping;

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
        stopping = false;
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        loadOptionalLocalFonts();

        providerMode = WeatherProviderFactory.providerMode();
        ProviderDiagnostics.info("Selected weather provider mode: " + providerMode + ".");
        service = newService(providerMode);
        remoteRefreshExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "aelia-remote-refresh-" + ThreadIds.NEXT.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });

        scene = new Scene(buildDashboardShell(WeatherProviderFactory.initialSnapshot(serviceProvider(), providerMode)), INITIAL_WIDTH, INITIAL_HEIGHT);
        URL stylesheet = AeliaApplication.class.getResource("/fr/alescis/aelia/styles/application.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Aelia");
        stage.setMinWidth(1180.0);
        stage.setMinHeight(760.0);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> stopServices());
        stage.show();

        if (WeatherProviderFactory.remoteRefreshEnabled(providerMode)) {
            scheduleRemoteRefresh(Duration.ofSeconds(1));
        }
    }

    @Override
    public void stop() {
        stopServices();
    }

    private AeliaWeatherService newService(String mode) {
        return new AeliaWeatherService(WeatherProviderFactory.createDashboardProvider(mode));
    }

    private WeatherDashboardProvider serviceProvider() {
        return service.provider();
    }

    private ScaledDashboardShell buildDashboardShell(DashboardSnapshot snapshot) {
        DashboardSnapshot normalized = snapshot.withDataStatus(snapshot.dataStatus().withProviderMode(providerMode));
        AeliaDashboardView dashboardView = new AeliaDashboardView(normalized, runtimeActions());
        return new ScaledDashboardShell(dashboardView);
    }

    private DashboardRuntimeActions runtimeActions() {
        return new DashboardRuntimeActions() {
            @Override
            public void selectProviderMode(String mode) {
                Platform.runLater(() -> switchProviderMode(mode));
            }

            @Override
            public void refreshProviderData() {
                Platform.runLater(() -> forceRefresh());
            }
        };
    }

    private void switchProviderMode(String requestedMode) {
        String normalizedMode = WeatherProviderFactory.normalizeMode(requestedMode);
        if (normalizedMode.equals(providerMode)) {
            forceRefresh();
            return;
        }
        ProviderDiagnostics.info("Switching weather provider mode from " + providerMode + " to " + normalizedMode + ".");
        cancelScheduledRefresh();
        closeCurrentService();
        providerMode = normalizedMode;
        service = newService(providerMode);
        consecutiveRefreshFailures = 0;
        if (WeatherProviderFactory.MODE_SIMULATED.equals(providerMode)) {
            scene.setRoot(buildDashboardShell(WeatherProviderFactory.simulatedSnapshot(providerMode)));
            return;
        }
        DashboardSnapshot startup = WeatherProviderFactory.simulatedSnapshot(providerMode)
                .withDataStatus(DashboardDataStatus.unavailable(providerMode, "Chargement des données distantes en cours."));
        scene.setRoot(buildDashboardShell(startup));
        scheduleRemoteRefresh(Duration.ZERO);
    }

    private void forceRefresh() {
        if (WeatherProviderFactory.remoteRefreshEnabled(providerMode)) {
            cancelScheduledRefresh();
            scheduleRemoteRefresh(Duration.ZERO);
        } else {
            scene.setRoot(buildDashboardShell(WeatherProviderFactory.simulatedSnapshot(providerMode)));
        }
    }

    private void scheduleRemoteRefresh(Duration delay) {
        if (stopping || remoteRefreshExecutor == null || remoteRefreshExecutor.isShutdown()) {
            return;
        }
        cancelScheduledRefresh();
        long delayMillis = Math.max(0L, delay.toMillis());
        scheduledRefresh = remoteRefreshExecutor.schedule(
                this::refreshSnapshot,
                delayMillis,
                TimeUnit.MILLISECONDS
        );
        ProviderDiagnostics.info("Next remote weather refresh scheduled in " + humanDelay(delay) + ".");
    }

    private void refreshSnapshot() {
        if (stopping) {
            return;
        }
        ProviderDiagnostics.info("Starting asynchronous remote weather refresh.");
        try {
            DashboardSnapshot loadedSnapshot = service.currentSnapshot();
            DashboardSnapshot snapshot = loadedSnapshot.withDataStatus(loadedSnapshot.dataStatus().withProviderMode(providerMode));
            consecutiveRefreshFailures = 0;
            Platform.runLater(() -> {
                if (!stopping) {
                    ProviderDiagnostics.info("Remote weather snapshot loaded for " + snapshot.currentWeather().city() + ".");
                    scene.setRoot(buildDashboardShell(snapshot));
                }
            });
            scheduleRemoteRefresh(SUCCESS_REFRESH_DELAY);
        } catch (RuntimeException exception) {
            consecutiveRefreshFailures++;
            Duration retryDelay = retryDelay(consecutiveRefreshFailures);
            ProviderDiagnostics.warn(
                    "Remote weather refresh failed. The visible snapshot remains unchanged; retrying in " + humanDelay(retryDelay) + ".",
                    exception
            );
            if (WeatherProviderFactory.MODE_OPEN_METEO.equals(providerMode)) {
                DashboardSnapshot errorSnapshot = WeatherProviderFactory.simulatedSnapshot(providerMode)
                        .withDataStatus(DashboardDataStatus.unavailable(providerMode, compactFailure(exception)));
                Platform.runLater(() -> {
                    if (!stopping) {
                        scene.setRoot(buildDashboardShell(errorSnapshot));
                    }
                });
            }
            scheduleRemoteRefresh(retryDelay);
        }
    }

    private Duration retryDelay(int failureCount) {
        return switch (Math.max(1, failureCount)) {
            case 1 -> Duration.ofSeconds(5);
            case 2 -> Duration.ofSeconds(15);
            case 3 -> Duration.ofSeconds(30);
            case 4 -> Duration.ofMinutes(1);
            case 5 -> Duration.ofMinutes(2);
            default -> Duration.ofMinutes(5);
        };
    }

    private String humanDelay(Duration delay) {
        long seconds = Math.max(0L, delay.toSeconds());
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        return remainingSeconds == 0L ? minutes + "min" : minutes + "min " + remainingSeconds + "s";
    }

    private void stopServices() {
        stopping = true;
        cancelScheduledRefresh();
        if (remoteRefreshExecutor != null) {
            remoteRefreshExecutor.shutdownNow();
            remoteRefreshExecutor = null;
        }
        closeCurrentService();
    }

    private void cancelScheduledRefresh() {
        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(false);
            scheduledRefresh = null;
        }
    }

    private void closeCurrentService() {
        if (service != null) {
            service.close();
            service = null;
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

    private String compactFailure(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String oneLine = message.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 180 ? oneLine.substring(0, 177) + "..." : oneLine;
    }

    private static final class ThreadIds {
        private static final AtomicInteger NEXT = new AtomicInteger();

        private ThreadIds() {
        }
    }
}
