package fr.alescis.aelia.model;

import java.util.Objects;

/**
 * User-visible data availability issue attached to a dashboard snapshot.
 */
public record DashboardDataIssue(String component, String message, boolean blocking) {
    public DashboardDataIssue {
        component = requireText(component, "component");
        message = requireText(message, "message");
    }

    public static DashboardDataIssue nonBlocking(String component, String message) {
        return new DashboardDataIssue(component, message, false);
    }

    public static DashboardDataIssue blocking(String component, String message) {
        return new DashboardDataIssue(component, message, true);
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return normalized;
    }
}
