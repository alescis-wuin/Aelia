package fr.alescis.aelia.model;

import java.util.Objects;

/**
 * Pollen level for a specific botanical group.
 */
public record PollenRisk(String name, PollenLevel level) {
    public PollenRisk {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pollen name is required.");
        }
        level = Objects.requireNonNull(level, "level");
    }
}
