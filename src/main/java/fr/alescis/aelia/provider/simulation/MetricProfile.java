package fr.alescis.aelia.provider.simulation;

/**
 * Stable simulated range definition for one metric.
 */
public record MetricProfile(
        String id,
        double baseValue,
        double amplitude,
        double minimum,
        double maximum,
        String unit
) {
    public MetricProfile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
        unit = unit == null ? "" : unit;
        if (amplitude < 0.0) {
            throw new IllegalArgumentException("Amplitude cannot be negative.");
        }
        if (maximum < minimum) {
            throw new IllegalArgumentException("Maximum must be greater than or equal to minimum.");
        }
    }
}
