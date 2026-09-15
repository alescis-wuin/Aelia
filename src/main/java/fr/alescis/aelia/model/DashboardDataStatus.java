package fr.alescis.aelia.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Describes the origin and reliability of the snapshot currently rendered by the dashboard.
 */
public record DashboardDataStatus(
        String providerMode,
        String sourceLabel,
        String details,
        List<DashboardDataIssue> issues,
        Instant updatedAt,
        boolean visible
) {
    public DashboardDataStatus {
        providerMode = normalize(providerMode, "auto");
        sourceLabel = normalize(sourceLabel, "Simulation");
        details = normalize(details, "Données disponibles.");
        issues = List.copyOf(issues == null ? List.of() : issues);
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public static DashboardDataStatus hidden(String providerMode) {
        return new DashboardDataStatus(providerMode, "Simulation", "Données locales simulées.", List.of(), Instant.now(), false);
    }

    public static DashboardDataStatus simulated(String providerMode) {
        return new DashboardDataStatus(providerMode, "Simulation", "Données locales simulées.", List.of(), Instant.now(), true);
    }

    public static DashboardDataStatus forecast(String providerMode) {
        return new DashboardDataStatus(providerMode, "API météo", "Données distantes chargées avec succès.", List.of(), Instant.now(), true);
    }

    public static DashboardDataStatus unavailable(String providerMode, String reason) {
        return new DashboardDataStatus(
                providerMode,
                "Simulation",
                compact(reason),
                List.of(DashboardDataIssue.blocking("API météo", compact(reason))),
                Instant.now(),
                true
        );
    }

    public static DashboardDataStatus remoteFailureFallback(String providerMode, String source, String reason) {
        return new DashboardDataStatus(
                providerMode,
                "Simulation",
                compact(reason),
                List.of(DashboardDataIssue.warning(source, compact(reason))),
                Instant.now(),
                true
        );
    }

    public static DashboardDataStatus secondaryForecastFallback(String providerMode, String source, String reason) {
        return new DashboardDataStatus(
                providerMode,
                source,
                "Repli distant utilisé : " + compact(reason),
                List.of(DashboardDataIssue.warning("Open-Meteo", compact(reason))),
                Instant.now(),
                true
        );
    }

    public static DashboardDataStatus historicalFallback(String providerMode, String reason) {
        return new DashboardDataStatus(
                providerMode,
                "Open-Meteo Archive",
                "Repli historique : " + compact(reason),
                List.of(DashboardDataIssue.warning("Prévision", compact(reason))),
                Instant.now(),
                true
        );
    }

    public DashboardDataStatus withProviderMode(String mode) {
        return new DashboardDataStatus(mode, sourceLabel, details, issues, updatedAt, visible);
    }

    public DashboardDataStatus withVisible(boolean nextVisible) {
        return new DashboardDataStatus(providerMode, sourceLabel, details, issues, updatedAt, nextVisible);
    }

    public boolean strictRemoteMode() {
        return "openmeteo".equals(providerMode) || "api".equals(providerMode);
    }

    public boolean hasBlockingIssues() {
        return issues.stream().anyMatch(DashboardDataIssue::blocking);
    }

    public String headline() {
        if (hasBlockingIssues()) {
            return "API indisponible";
        }
        if (!issues.isEmpty()) {
            return "Repli actif";
        }
        return sourceLabel;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return Objects.requireNonNull(fallback, "fallback");
        }
        return value.trim();
    }

    private static String compact(String value) {
        if (value == null || value.isBlank()) {
            return "Raison inconnue.";
        }
        String oneLine = value.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 180 ? oneLine.substring(0, 177) + "..." : oneLine;
    }
}
