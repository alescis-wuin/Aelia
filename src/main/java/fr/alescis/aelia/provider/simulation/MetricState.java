package fr.alescis.aelia.provider.simulation;

import java.time.Instant;
import java.util.Objects;

/**
 * Last simulated value produced for a metric.
 */
public record MetricState(MetricProfile profile, double value, Instant measuredAt) {
    public MetricState {
        profile = Objects.requireNonNull(profile, "profile");
        measuredAt = Objects.requireNonNull(measuredAt, "measuredAt");
    }
}
