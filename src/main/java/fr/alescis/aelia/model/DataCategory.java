package fr.alescis.aelia.model;

/**
 * High-level groups used to organize weather and environmental metrics.
 */
public enum DataCategory {
    WEATHER("Weather"),
    ATMOSPHERE("Atmosphere"),
    SUN("Sun"),
    AIR("Air"),
    HEALTH("Health");

    private final String label;

    DataCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
