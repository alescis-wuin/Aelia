package fr.seynax.aelia.model;

/**
 * High-level metric groups displayed by the UI.
 */
public enum DataCategory {
    WEATHER("Weather"),
    ASTRONOMY("Astronomy"),
    AIR_QUALITY("Air quality"),
    POLLEN("Pollen");

    private final String label;

    DataCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
