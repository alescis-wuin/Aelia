package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.DashboardSnapshot;

/**
 * Backward-compatible dashboard view name.
 */
public final class DashboardView extends AeliaDashboardView {
    public DashboardView(DashboardSnapshot snapshot) {
        super(snapshot);
    }
}
