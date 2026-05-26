package fr.alescis.aelia.model;

import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Immutable description of a data point supported by a provider.
 */
public record DataMetric(
        String id,
        String displayName,
        DataCategory category,
        DataKind kind,
        String unit,
        OptionalDouble minimum,
        OptionalDouble maximum,
        int decimals,
        String description
) {
    public DataMetric {
        id = requireIdentifier(id);
        displayName = requireText(displayName, "displayName");
        category = Objects.requireNonNull(category, "category");
        kind = Objects.requireNonNull(kind, "kind");
        unit = Objects.requireNonNull(unit, "unit").trim();
        minimum = Objects.requireNonNull(minimum, "minimum");
        maximum = Objects.requireNonNull(maximum, "maximum");
        if (decimals < 0 || decimals > 3) {
            throw new IllegalArgumentException("decimals must be between 0 and 3");
        }
        description = requireText(description, "description");
        if (minimum.isPresent() && maximum.isPresent() && minimum.getAsDouble() > maximum.getAsDouble()) {
            throw new IllegalArgumentException("minimum must not be greater than maximum");
        }
    }

    public static DataMetric numeric(
            String id,
            String displayName,
            DataCategory category,
            String unit,
            double minimum,
            double maximum,
            int decimals,
            String description
    ) {
        return new DataMetric(
                id,
                displayName,
                category,
                DataKind.NUMERIC,
                unit,
                OptionalDouble.of(minimum),
                OptionalDouble.of(maximum),
                decimals,
                description
        );
    }

    public static DataMetric text(
            String id,
            String displayName,
            DataCategory category,
            DataKind kind,
            String description
    ) {
        if (kind == DataKind.NUMERIC) {
            throw new IllegalArgumentException("Use numeric factory for numeric metrics");
        }
        return new DataMetric(
                id,
                displayName,
                category,
                kind,
                "",
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                0,
                description
        );
    }

    public String rangeLabel() {
        if (minimum.isEmpty() || maximum.isEmpty()) {
            return "—";
        }
        return formatBound(minimum.getAsDouble()) + "…" + formatBound(maximum.getAsDouble()) + unitSuffix();
    }

    public String unitSuffix() {
        return unit.isBlank() ? "" : " " + unit;
    }

    private String formatBound(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static String requireIdentifier(String value) {
        String normalized = requireText(value, "id");
        if (!normalized.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("id must match [a-z][a-z0-9_]*");
        }
        return normalized;
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
