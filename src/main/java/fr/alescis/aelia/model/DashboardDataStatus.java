package fr.alescis.aelia.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Describes the source and reliability of the values rendered by the dashboard.
 */
public record DashboardDataStatus(
        String providerMode,
        String sourceLabel,
        boolean remoteRequested,
        boolean forecastAvailable,
        boolean archiveFallbackUsed,
        List<DashboardDataIssue> issues
) {
    public DashboardDataStatus {
        providerMode = normalizedMode(providerMode);
        sourceLabel = sourceLabel == null || sourceLabel.isBlank() ? "Simulation" : sourceLabel.trim();
        issues = List.copyOf(Objects.requireNonNull(issues, "issues"));
    }

    public static DashboardDataStatus simulated(String providerMode) {
        return new DashboardDataStatus(providerMode, "Simulation", false, false, false, List.of());
    }

    public static DashboardDataStatus forecast(String providerMode) {
        return new DashboardDataStatus(providerMode, "Open-Meteo Forecast", true, true, false, List.of());
    }

    public static DashboardDataStatus secondaryForecastFallback(String providerMode, String sourceLabel, String failureSummary) {
        String normalizedSource = sourceLabel == null || sourceLabel.isBlank() ? "Source météo secondaire" : sourceLabel.trim();
        String normalizedFailure = failureSummary == null || failureSummary.isBlank()
                ? "Open-Meteo Forecast is unavailable."
                : failureSummary.trim();
        List<DashboardDataIssue> issueList = new ArrayList<>();
        issueList.add(DashboardDataIssue.blocking("Open-Meteo Forecast", normalizedFailure));
        issueList.add(DashboardDataIssue.nonBlocking("Prévisions", "Les prévisions visibles proviennent de " + normalizedSource + "."));
        if (normalizedSource.toLowerCase(java.util.Locale.ROOT).contains("met norway")) {
            issueList.add(DashboardDataIssue.blocking("Air / pollens", "MET Norway Locationforecast ne fournit pas la qualité de l'air ni les pollens."));
            issueList.add(DashboardDataIssue.blocking("Soleil", "MET Norway Locationforecast ne fournit pas directement les heures de lever et coucher du soleil."));
        } else if (normalizedSource.toLowerCase(java.util.Locale.ROOT).contains("weatherapi")) {
            issueList.add(DashboardDataIssue.blocking("Pollens", "Les pollens WeatherAPI sont réservés aux offres supérieures ; la carte peut rester indisponible."));
        }
        return new DashboardDataStatus(providerMode, normalizedSource, true, false, true, issueList);
    }

    public static DashboardDataStatus archiveFallback(String providerMode, String failureSummary) {
        String normalizedFailure = failureSummary == null || failureSummary.isBlank()
                ? "Open-Meteo Forecast is unavailable."
                : failureSummary.trim();
        return new DashboardDataStatus(
                providerMode,
                "Open-Meteo Historical Weather",
                true,
                false,
                true,
                List.of(
                        DashboardDataIssue.blocking("Prévisions", normalizedFailure),
                        DashboardDataIssue.blocking("Horaire", "Les valeurs horaires affichées proviennent d'une journée historique comparable, pas d'une prévision."),
                        DashboardDataIssue.blocking("7 jours", "La tendance affichée est historique. Les prévisions futures sont indisponibles."),
                        DashboardDataIssue.blocking("Air / pollens / UV", "Ces cartes nécessitent Forecast ou Air Quality et peuvent rester simulées ou indisponibles.")
                )
        );
    }

    public static DashboardDataStatus unavailable(String providerMode, String failureSummary) {
        String normalizedFailure = failureSummary == null || failureSummary.isBlank()
                ? "Aucune API distante Open-Meteo n'est disponible."
                : failureSummary.trim();
        return new DashboardDataStatus(
                providerMode,
                "Indisponible",
                true,
                false,
                false,
                List.of(DashboardDataIssue.blocking("Open-Meteo", normalizedFailure))
        );
    }

    public DashboardDataStatus withProviderMode(String mode) {
        return new DashboardDataStatus(mode, sourceLabel, remoteRequested, forecastAvailable, archiveFallbackUsed, issues);
    }

    public boolean strictRemoteMode() {
        return "openmeteo".equals(providerMode);
    }

    public boolean hasBlockingIssues() {
        return issues.stream().anyMatch(DashboardDataIssue::blocking);
    }

    public boolean visible() {
        return remoteRequested || !issues.isEmpty();
    }

    public String headline() {
        if (archiveFallbackUsed) {
            if (sourceLabel.toLowerCase(java.util.Locale.ROOT).contains("historical")) {
                return strictRemoteMode()
                        ? "Prévisions Open-Meteo indisponibles · données historiques affichées"
                        : "Open-Meteo Forecast indisponible · repli historique actif";
            }
            return strictRemoteMode()
                    ? "Open-Meteo Forecast indisponible · source secondaire active"
                    : "Repli météo secondaire actif";
        }
        if (forecastAvailable) {
            return "Données réelles Open-Meteo";
        }
        if (remoteRequested && hasBlockingIssues()) {
            return "API distante indisponible";
        }
        return "Données simulées";
    }

    public String details() {
        if (issues.isEmpty()) {
            return sourceLabel;
        }
        return issues.stream()
                .map(issue -> issue.component() + " : " + issue.message())
                .reduce((left, right) -> left + " · " + right)
                .orElse(sourceLabel);
    }

    private static String normalizedMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "auto";
        }
        String normalized = mode.trim().toLowerCase(java.util.Locale.ROOT).replace("_", "-");
        return switch (normalized) {
            case "simulated", "openmeteo", "auto" -> normalized;
            default -> "auto";
        };
    }
}
