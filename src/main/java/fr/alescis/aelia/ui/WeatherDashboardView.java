package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.DashboardSnapshot;

/**
 * Backward-compatible dashboard view name used by earlier prototypes.
 */
public final class WeatherDashboardView extends AeliaDashboardView {
    public WeatherDashboardView(DashboardSnapshot snapshot) {
        super(snapshot);
    }
}
