package fr.alescis.aelia;

import atlantafx.base.theme.PrimerDark;
import fr.alescis.aelia.model.DashboardDataStatus;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.LocationWeather;
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
    private static final List<String> STYLESHEETS = List.of(
            "/fr/alescis/aelia/styles/application.css",
            "/fr/alescis/aelia/styles/provider.css"
    );

    private volatile AeliaWeatherService service;
    private volatile String providerMode;
    private volatile boolean stopping;
    private AeliaDashboardView dashboardView;
    private ScheduledExecutorService runtimeExecutor;
    private ScheduledFuture<?> scheduledRefresh;
    private int consecutiveRefreshFailures;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stopping = false;
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        loadOptionalLocalFonts();

        runtimeExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "aelia-runtime-provider-" + ThreadIds.NEXT.incrementAndGet());
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
            scheduleProviderRefresh(Duration.ofSeconds(1));
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
        cancelScheduledRefresh();
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
            consecutiveRefreshFailures = 0;
            Platform.runLater(() -> dashboardView.updateSnapshot(snapshot));
            if (WeatherProviderFactory.remoteRefreshEnabled(normalizedMode)) {
                scheduleProviderRefresh(Duration.ZERO);
            }
        });
    }

    private void selectWeatherLocation(LocationWeather location) {
        if (location == null || !location.hasCoordinates()) {
            return;
        }
        String activeMode = providerMode;
        if (!WeatherProviderFactory.remoteRefreshEnabled(activeMode)) {
            return;
        }
        System.setProperty("aelia.openmeteo.latitude", Double.toString(location.latitude()));
        System.setProperty("aelia.openmeteo.longitude", Double.toString(location.longitude()));
        System.setProperty("aelia.openmeteo.city", location.city());
        System.setProperty("aelia.openmeteo.country", location.country());
        ProviderDiagnostics.info("Selected weather location: " + location.city()
                + " (" + location.latitude() + ", " + location.longitude() + ").");
        cancelScheduledRefresh();
        runtimeExecutor.execute(() -> {
            AeliaWeatherService previousService = service;
            if (previousService != null) {
                previousService.close();
            }
            WeatherDashboardProvider provider = WeatherProviderFactory.createDashboardProvider(activeMode);
            service = new AeliaWeatherService(provider);
            providerMode = activeMode;
            consecutiveRefreshFailures = 0;
            scheduleProviderRefresh(Duration.ZERO);
        });
    }

    private void refreshProviderData() {
        cancelScheduledRefresh();
        scheduleProviderRefresh(Duration.ZERO);
    }

    private void scheduleProviderRefresh(Duration delay) {
        if (stopping || runtimeExecutor == null || runtimeExecutor.isShutdown()) {
            return;
        }
        cancelScheduledRefresh();
        long delayMillis = Math.max(0L, delay.toMillis());
        scheduledRefresh = runtimeExecutor.schedule(this::refreshProviderDataNow, delayMillis, TimeUnit.MILLISECONDS);
        ProviderDiagnostics.info("Next remote weather refresh scheduled in " + humanDelay(delay) + ".");
    }

    private void refreshProviderDataNow() {
        AeliaWeatherService activeService = service;
        String activeMode = providerMode;
        if (stopping || activeService == null) {
            return;
        }
        if (!WeatherProviderFactory.remoteRefreshEnabled(activeMode)) {
            Platform.runLater(() -> dashboardView.updateSnapshot(WeatherProviderFactory.simulatedSnapshot(activeMode)));
            return;
        }
        ProviderDiagnostics.info("Starting asynchronous remote weather refresh for mode " + activeMode + ".");
        try {
            DashboardSnapshot snapshot = activeService.currentSnapshot();
            DashboardSnapshot normalizedSnapshot = snapshot.withDataStatus(snapshot.dataStatus().withProviderMode(activeMode));
            consecutiveRefreshFailures = 0;
            Platform.runLater(() -> {
                if (!stopping) {
                    dashboardView.updateSnapshot(normalizedSnapshot);
                }
            });
            scheduleProviderRefresh(SUCCESS_REFRESH_DELAY);
        } catch (RuntimeException exception) {
            consecutiveRefreshFailures++;
            Duration retryDelay = retryDelay(consecutiveRefreshFailures);
            ProviderDiagnostics.warn("Remote weather refresh failed; retrying in " + humanDelay(retryDelay) + ".", exception);
            DashboardSnapshot fallback = WeatherProviderFactory.simulatedSnapshot(activeMode).withDataStatus(
                    DashboardDataStatus.remoteFailureFallback(activeMode, "API météo", WeatherProviderFactory.compactFailure(exception))
            );
            Platform.runLater(() -> {
                if (!stopping) {
                    dashboardView.updateSnapshot(fallback);
                }
            });
            scheduleProviderRefresh(retryDelay);
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

    private void closeService() {
        stopping = true;
        cancelScheduledRefresh();
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

    private void cancelScheduledRefresh() {
        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(false);
            scheduledRefresh = null;
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

        @Override
        public void selectWeatherLocation(LocationWeather location) {
            AeliaApplication.this.selectWeatherLocation(location);
        }
    }

    private static final class ThreadIds {
        private static final AtomicInteger NEXT = new AtomicInteger();

        private ThreadIds() {
        }
    }
}
