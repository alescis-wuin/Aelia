package fr.alescis.aelia.ui.components;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

/**
 * Centralizes tooltip creation so every interactive dashboard element uses the same delay and behavior.
 */
public final class TooltipSupport {
    private static final double SHOW_DELAY_MILLIS = 550.0;
    private static final double HIDE_DELAY_MILLIS = 90.0;
    private static final double SHOW_DURATION_SECONDS = 18.0;
    private static final double MAX_WIDTH = 360.0;

    private TooltipSupport() {
    }

    public static Tooltip install(Node node, String text) {
        if (node == null || text == null || text.isBlank()) {
            return null;
        }
        Tooltip tooltip = new Tooltip(text.trim());
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(MAX_WIDTH);
        tooltip.setShowDelay(Duration.millis(SHOW_DELAY_MILLIS));
        tooltip.setHideDelay(Duration.millis(HIDE_DELAY_MILLIS));
        tooltip.setShowDuration(Duration.seconds(SHOW_DURATION_SECONDS));
        Tooltip.install(node, tooltip);
        return tooltip;
    }
}
