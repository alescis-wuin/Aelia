package fr.alescis.aelia.provider;

import fr.alescis.aelia.model.DashboardDataStatus;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.provider.openmeteo.OpenMeteoWeatherDashboardProvider;
import fr.alescis.aelia.provider.simulation.SimulatedWeatherDashboardProvider;

import java.util.Locale;

/**
 * Creates dashboard providers from runtime settings without coupling the JavaFX entry point to one backend.
 */
public final class WeatherProviderFactory {
    public static final String MODE_SIMULATED = "simulated";
    public static final String MODE_OPEN_METEO = "openmeteo";
    public static final String MODE_AUTO = "auto";

    private static final String PROPERTY_NAME = "aelia.weather.provider";
    private static final String ENVIRONMENT_NAME = "AELIA_WEATHER_PROVIDER";

    private WeatherProviderFactory() {
    }

    public static WeatherDashboardProvider createDashboardProvider() {
        return createDashboardProvider(providerMode());
    }

    public static WeatherDashboardProvider createDashboardProvider(String mode) {
        return switch (normalizeMode(mode)) {
            case MODE_SIMULATED -> new SimulatedWeatherDashboardProvider();
            case MODE_OPEN_METEO, MODE_AUTO -> new OpenMeteoWeatherDashboardProvider();
            default -> throw new IllegalStateException("Unexpected provider mode.");
        };
    }

    public static DashboardSnapshot initialSnapshot(WeatherDashboardProvider provider, String mode) {
        String normalizedMode = normalizeMode(mode);
        if (MODE_OPEN_METEO.equals(normalizedMode)) {
            ProviderDiagnostics.info("Loading initial Open-Meteo snapshot synchronously.");
            try {
                DashboardSnapshot snapshot = provider.currentSnapshot();
                return snapshot.withDataStatus(snapshot.dataStatus().withProviderMode(normalizedMode));
            } catch (RuntimeException exception) {
                ProviderDiagnostics.warn("Initial Open-Meteo snapshot failed. Aelia will start with the simulated snapshot and keep retrying asynchronously.", exception);
                return simulatedSnapshot(normalizedMode).withDataStatus(DashboardDataStatus.unavailable(normalizedMode, compactFailure(exception)));
            }
        }
        return simulatedSnapshot(normalizedMode);
    }

    public static DashboardSnapshot initialSnapshot(WeatherDashboardProvider provider) {
        return initialSnapshot(provider, providerMode());
    }

    public static DashboardSnapshot simulatedSnapshot(String mode) {
        try (SimulatedWeatherDashboardProvider simulatedProvider = new SimulatedWeatherDashboardProvider()) {
            return simulatedProvider.currentSnapshot().withDataStatus(DashboardDataStatus.simulated(normalizeMode(mode)));
        }
    }

    public static boolean remoteRefreshEnabled(String mode) {
        String normalized = normalizeMode(mode);
        return MODE_AUTO.equals(normalized) || MODE_OPEN_METEO.equals(normalized);
    }

    public static boolean remoteRefreshEnabled() {
        return remoteRefreshEnabled(providerMode());
    }

    public static String providerMode() {
        return normalizeMode(EnvironmentSettings.text(PROPERTY_NAME, ENVIRONMENT_NAME).orElse(null));
    }

    public static String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return MODE_AUTO;
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT).replace("_", "-");
        if (MODE_SIMULATED.equals(normalized) || MODE_OPEN_METEO.equals(normalized) || MODE_AUTO.equals(normalized)) {
            return normalized;
        }
        ProviderDiagnostics.warn("Unsupported provider mode '" + mode + "'. Falling back to auto mode.", null);
        return MODE_AUTO;
    }

    private static String compactFailure(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String oneLine = message.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 180 ? oneLine.substring(0, 177) + "..." : oneLine;
    }
}
