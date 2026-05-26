package fr.alescis.aelia.model;

/**
 * Normalized weather conditions used by the UI and providers.
 */
public enum WeatherCondition {
    SUNNY("Ensoleillé"),
    PARTLY_CLOUDY("Nuageux"),
    RAINY("Pluvieux"),
    CLEAR_NIGHT("Nuit claire");

    private final String label;

    WeatherCondition(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
