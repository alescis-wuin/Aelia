package fr.alescis.aelia.model;

/**
 * Provider limit visible in the API limits reference section.
 */
public record ApiLimit(String name, String period, String value) {
    public ApiLimit {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Limit name is required.");
        }
        if (period == null || period.isBlank()) {
            throw new IllegalArgumentException("Limit period is required.");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Limit value is required.");
        }
    }

    /**
     * Creates a metered limit entry from a structured period.
     *
     * @param name displayed limit name
     * @param period measurement window
     * @param amount allowed amount for the window
     * @param unit displayed unit
     * @return immutable API limit entry
     */
    public static ApiLimit limited(String name, LimitPeriod period, int amount, String unit) {
        if (period == null) {
            throw new IllegalArgumentException("Limit period is required.");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Limit amount must be positive or zero.");
        }
        String normalizedUnit = unit == null || unit.isBlank() ? "requests" : unit.trim();
        return new ApiLimit(name, period.label(), amount + " " + normalizedUnit);
    }

    /**
     * Creates an unmetered limit entry from a structured period.
     *
     * @param name displayed limit name
     * @param period measurement window
     * @param unit displayed unit
     * @return immutable API limit entry
     */
    public static ApiLimit unmetered(String name, LimitPeriod period, String unit) {
        if (period == null) {
            throw new IllegalArgumentException("Limit period is required.");
        }
        String normalizedUnit = unit == null || unit.isBlank() ? "requests" : unit.trim();
        return new ApiLimit(name, period.label(), "unmetered " + normalizedUnit);
    }
}
