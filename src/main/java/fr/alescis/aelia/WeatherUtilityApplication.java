package fr.alescis.aelia;

/**
 * Backward-compatible launcher kept for older run configurations.
 */
public final class WeatherUtilityApplication {
    private WeatherUtilityApplication() {
    }

    public static void main(String[] args) {
        AeliaApplication.main(args);
    }
}
