package fr.alescis.aelia.model;

/**
 * Pollen risk scale displayed by the dashboard.
 */
public enum PollenLevel {
    NONE("Nul", 0.05),
    LOW("Faible", 0.25),
    MODERATE("Moyen", 0.50),
    HIGH("Fort", 0.80),
    EXTREME("Extrême", 1.00);

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
