package fr.alescis.aelia.provider;

import fr.alescis.aelia.model.DashboardDataStatus;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.provider.remote.RemoteWeatherDashboardProvider;
import fr.alescis.aelia.provider.simulation.SimulatedWeatherDashboardProvider;

import java.util.Locale;

/**
 * Creates dashboard providers from runtime settings without coupling JavaFX startup to one backend.
 */
public final class WeatherProviderFactory {
    public static final String MODE_SIMULATED = "simulated";
    public static final String MODE_SIMULATION = "simulation";
    public static final String MODE_AUTO = "auto";
    public static final String MODE_API = "api";
    public static final String MODE_OPEN_METEO = "openmeteo";

    private static final String PROPERTY_NAME = "aelia.weather.provider";
    private static final String ENVIRONMENT_NAME = "AELIA_WEATHER_PROVIDER";

    private WeatherProviderFactory() {
    }

    public static WeatherDashboardProvider createDashboardProvider() {
        return createDashboardProvider(providerMode());
    }

    public static WeatherDashboardProvider createDashboardProvider(String mode) {
        String normalized = normalizeMode(mode);
        if (MODE_SIMULATED.equals(normalized)) {
            return new SimulatedWeatherDashboardProvider();
        }
        return new RemoteWeatherDashboardProvider(normalized);
    }

    public static DashboardSnapshot initialSnapshot(WeatherDashboardProvider provider, String mode) {
        String normalizedMode = normalizeMode(mode);
        if (MODE_API.equals(normalizedMode)) {
            ProviderDiagnostics.info("Loading initial remote API snapshot synchronously.");
            try {
                DashboardSnapshot snapshot = provider.currentSnapshot();
                return snapshot.withDataStatus(snapshot.dataStatus().withProviderMode(normalizedMode));
            } catch (RuntimeException exception) {
                ProviderDiagnostics.warn("Initial remote API snapshot failed. Aelia starts with the simulated snapshot and can retry asynchronously.", exception);
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
        return MODE_AUTO.equals(normalized) || MODE_API.equals(normalized);
    }

    public static boolean remoteRefreshEnabled() {
        return remoteRefreshEnabled(providerMode());
    }

    public static String providerMode() {
        String environmentValue = System.getenv(ENVIRONMENT_NAME);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return normalizeMode(environmentValue);
        }
        return normalizeMode(EnvironmentSettings.text(PROPERTY_NAME, ENVIRONMENT_NAME).orElse(null));
    }

    public static String normalizeMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return MODE_AUTO;
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT).replace('_', '-').replace('.', '-');
        return switch (normalized) {
            case MODE_SIMULATED, MODE_SIMULATION -> MODE_SIMULATED;
            case MODE_AUTO -> MODE_AUTO;
            case MODE_API, MODE_OPEN_METEO, "remote", "distant", "api-remote" -> MODE_API;
            default -> {
                ProviderDiagnostics.warn("Unsupported provider mode '" + mode + "'. Falling back to auto mode.", null);
                yield MODE_AUTO;
            }
        };
    }

    public static String compactFailure(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        String oneLine = message.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 180 ? oneLine.substring(0, 177) + "..." : oneLine;
    }
}
