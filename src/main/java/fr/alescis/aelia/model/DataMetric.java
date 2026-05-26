package fr.alescis.aelia.model;

import java.util.Objects;

/**
 * Structured metric descriptor independent from the presentation model.
 */
public record DataMetric(
        String id,
        String label,
        DataCategory category,
        DataKind kind,
        String unit
) {
    public DataMetric {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Metric id is required.");
        }
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Metric label is required.");
        }
        category = Objects.requireNonNull(category, "category");
        kind = Objects.requireNonNull(kind, "kind");
        unit = unit == null ? "" : unit.trim();
    }

    public static DataMetric numeric(String id, String label, DataCategory category, String unit) {
        return new DataMetric(id, label, category, DataKind.NUMERIC, unit);
    }

    public static DataMetric text(String id, String label, DataCategory category) {
        return new DataMetric(id, label, category, DataKind.TEXT, "");
    }

    public String displayName() {
        return label;
    }
}
