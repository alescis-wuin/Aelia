package fr.alescis.aelia.model;

import java.util.Objects;

/**
 * Describes one provider issue shown in the dashboard status banner.
 */
public record DashboardDataIssue(String source, String message, boolean blocking) {
    public DashboardDataIssue {
        source = source == null || source.isBlank() ? "Données" : source.trim();
        message = Objects.requireNonNullElse(message, "Indisponible").trim();
        if (message.isBlank()) {
            message = "Indisponible";
        }
    }

    public static DashboardDataIssue warning(String source, String message) {
        return new DashboardDataIssue(source, message, false);
    }

    public static DashboardDataIssue blocking(String source, String message) {
        return new DashboardDataIssue(source, message, true);
    }
}
