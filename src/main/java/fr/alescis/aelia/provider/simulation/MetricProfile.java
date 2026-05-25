package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.DataMetric;

/**
 * Simulation parameters attached to a numeric metric.
 */
record MetricProfile(
        DataMetric metric,
        double initialMinimum,
        double initialMaximum,
        double targetMinimum,
        double targetMaximum,
        double volatility,
        double attraction,
        double maximumStepPerSecond
) {
    MetricProfile {
        if (initialMinimum > initialMaximum) {
            throw new IllegalArgumentException("Initial bounds are invalid for " + metric.id());
        }
        if (targetMinimum > targetMaximum) {
            throw new IllegalArgumentException("Target bounds are invalid for " + metric.id());
        }
        if (volatility < 0.0 || attraction < 0.0 || maximumStepPerSecond <= 0.0) {
            throw new IllegalArgumentException("Simulation parameters must be positive for " + metric.id());
        }
    }
}
