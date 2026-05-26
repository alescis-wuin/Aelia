package fr.alescis.aelia.model;

/**
 * UI-neutral weather condition categories.
 */
public enum WeatherCondition {
    SUNNY("Ensoleillé"),
    PARTLY_CLOUDY("Nuageux"),
    CLOUDY("Couvert"),
    RAINY("Pluvieux"),
    CLEAR_NIGHT("Nuit claire"),
    STORMY("Orageux"),
    SNOWY("Neigeux");

    private final String label;

    WeatherCondition(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
