package fr.alescis.aelia.model;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable description of a provider metric.
 */
public record DataMetric(
        String id,
        String displayName,
        DataCategory category,
        DataKind kind,
        String unit,
        double minimum,
        double maximum,
        String description
) {
    public DataMetric {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(description, "description");
        if (!id.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Metric id must use lower snake case: " + id);
        }
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Metric display name must not be blank.");
        }
        if (kind == DataKind.NUMERIC && (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum >= maximum)) {
            throw new IllegalArgumentException("Numeric metric bounds are invalid for " + id);
        }
    }

    public String rangeText() {
        if (kind != DataKind.NUMERIC) {
            return "state";
        }
        return String.format(Locale.ROOT, "%.1f to %.1f %s", minimum, maximum, unit).trim();
    }
}
