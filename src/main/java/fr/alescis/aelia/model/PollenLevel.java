package fr.alescis.aelia.model;

/**
 * Normalized pollen risk levels used for bars and legends.
 */
public enum PollenLevel {
    NONE("Nul", 0.08),
    LOW("Faible", 0.22),
    MODERATE("Moyen", 0.50),
    HIGH("Fort", 0.80),
    EXTREME("Extrême", 1.0);

    private final String label;
    private final double normalizedValue;

    PollenLevel(String label, double normalizedValue) {
        this.label = label;
        this.normalizedValue = normalizedValue;
    }

    public String label() {
        return label;
    }

    public double normalizedValue() {
        return normalizedValue;
    }
}
