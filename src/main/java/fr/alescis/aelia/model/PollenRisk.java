package fr.alescis.aelia.model;

/**
 * Pollen level for a specific botanical group.
 */
public record PollenRisk(String name, PollenLevel level) {
    public PollenRisk {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pollen name is required.");
        }
        if (level == null) {
            throw new IllegalArgumentException("Pollen level is required.");
        }
    }
}
