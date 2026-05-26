package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.WeatherCondition;

import java.util.Objects;

/**
 * Simulated condition label plus normalized weather category.
 */
public record WeatherConditionState(WeatherCondition condition, String label) {
    public WeatherConditionState {
        condition = Objects.requireNonNull(condition, "condition");
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Condition label is required.");
        }
    }
}
