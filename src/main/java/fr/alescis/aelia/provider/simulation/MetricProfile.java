package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.DataMetric;

import java.time.Duration;
import java.util.Objects;

/**
 * Numeric simulation parameters used by one metric state.
 */
record MetricProfile(
        DataMetric metric,
        double baseValue,
        double minimum,
        double maximum,
        double volatility,
        double recovery,
        double cycleAmplitude,
        Duration cycleDuration
) {
    MetricProfile {
        metric = Objects.requireNonNull(metric, "metric");
        cycleDuration = Objects.requireNonNull(cycleDuration, "cycleDuration");
        if (minimum > maximum) {
            throw new IllegalArgumentException("minimum must not be greater than maximum");
        }
        if (baseValue < minimum || baseValue > maximum) {
            throw new IllegalArgumentException("baseValue must be inside the profile range");
        }
        if (volatility < 0.0d || recovery < 0.0d || cycleAmplitude < 0.0d) {
            throw new IllegalArgumentException("profile values must be positive");
        }
        if (cycleDuration.isNegative() || cycleDuration.isZero()) {
            throw new IllegalArgumentException("cycleDuration must be positive");
        }
    }
}
