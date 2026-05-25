package fr.seynax.aelia.model;

import java.util.Objects;
import java.util.OptionalLong;

/**
 * Describes a provider limit that the UI can expose to users.
 */
public record ApiLimit(
        String name,
        LimitPeriod period,
        OptionalLong limit,
        String unit,
        String notes
) {
    public ApiLimit {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(limit, "limit");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(notes, "notes");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Limit name must not be blank.");
        }
        if (unit.isBlank()) {
            throw new IllegalArgumentException("Limit unit must not be blank.");
        }
    }

    public String formattedLimit() {
        return limit.isPresent() ? Long.toString(limit.getAsLong()) : "unlimited";
    }
}
