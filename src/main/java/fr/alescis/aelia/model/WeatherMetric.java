package fr.alescis.aelia.model;

/**
 * Publicly supported data point exposed by a provider.
 */
public record WeatherMetric(
        String id,
        String label,
        String category,
        String unit,
        String supportedPeriod,
        double minimumRealisticValue,
        double maximumRealisticValue
) {
    public WeatherMetric {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Metric label is required.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Metric category is required.");
        }
        unit = unit == null ? "" : unit;
        supportedPeriod = supportedPeriod == null ? "" : supportedPeriod;
        if (maximumRealisticValue < minimumRealisticValue) {
            throw new IllegalArgumentException("Metric maximum must be greater than or equal to minimum.");
        }
    }

    /**
     * Compatibility alias for the upper realistic bound used by dashboard views.
     *
     * @return maximum realistic value
     */
    public double maximum() {
        return maximumRealisticValue;
    }

    /**
     * Compatibility alias for the lower realistic bound used by dashboard views.
     *
     * @return minimum realistic value
     */
    public double minimum() {
        return minimumRealisticValue;
    }
}
