package fr.alescis.aelia.ui.components;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Centers and scales the mockup-size dashboard while preserving its proportions.
 */
public final class ScaledDashboardShell extends StackPane {
    private static final double DESIGN_WIDTH = 1374.0;
    private static final double DESIGN_HEIGHT = 854.0;

    public ScaledDashboardShell(Node dashboard) {
        getStyleClass().add("scaled-dashboard-shell");
        setAlignment(Pos.CENTER);
        getChildren().add(dashboard);
        NumberBinding scale = Bindings.min(widthProperty().divide(DESIGN_WIDTH), heightProperty().divide(DESIGN_HEIGHT));
        dashboard.scaleXProperty().bind(scale);
        dashboard.scaleYProperty().bind(scale);
    }
}
