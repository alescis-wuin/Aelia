package fr.alescis.aelia.model;

/**
 * Measurement window used to describe provider or remote API limits.
 */
public enum LimitPeriod {
    SECOND("seconde"),
    MINUTE("minute"),
    HOUR("heure"),
    DAY("jour"),
    WEEK("semaine"),
    MONTH("mois"),
    CLIENT("client"),
    STREAM("flux"),
    NETWORK("réseau"),
    LOCAL("local"),
    CUSTOM("personnalisé");

    private final String label;

    LimitPeriod(String label) {
        this.label = label;
    }

    /**
     * Returns the localized display label used by the dashboard.
     *
     * @return display label
     */
    public String label() {
        return label;
    }
}
