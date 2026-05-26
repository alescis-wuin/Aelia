package fr.alescis.aelia.model;

/**
 * High-level grouping used by providers and future settings screens.
 */
public enum DataCategory {
    WEATHER("Météo"),
    ATMOSPHERE("Atmosphère"),
    HEALTH("Santé"),
    AIR_QUALITY("Qualité de l'air"),
    POLLEN("Pollens"),
    ASTRONOMY("Astronomie");

    private final String label;

    DataCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
