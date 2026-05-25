package fr.seynax.aelia.model;

/**
 * Time window used by an API limit.
 */
public enum LimitPeriod {
    SECOND("per second"),
    MINUTE("per minute"),
    HOUR("per hour"),
    DAY("per day"),
    WEEK("per week"),
    MONTH("per month"),
    LOCAL("local runtime");

    private final String label;

    LimitPeriod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
