package fr.alescis.aelia.model;

/**
 * Human readable period used to display provider limits.
 */
public enum LimitPeriod {
    SECOND("seconde"),
    MINUTE("minute"),
    HOUR("heure"),
    DAY("jour"),
    MONTH("mois"),
    CLIENT("client"),
    STREAM("flux"),
    NETWORK("réseau");

    private final String label;

    LimitPeriod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
