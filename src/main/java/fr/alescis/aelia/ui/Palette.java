package fr.alescis.aelia.ui;

import javafx.scene.paint.Color;

/**
 * Centralized color palette for the Aelia mockup.
 */
public final class Palette {
    public static final Color BACKGROUND = Color.web("#070B14");
    public static final Color SURFACE = Color.web("#0E1724");
    public static final Color SURFACE_ALT = Color.web("#101B31");
    public static final Color BORDER = Color.web("#1E2E48");
    public static final Color BORDER_SOFT = Color.web("#1E2A3A");
    public static final Color TEXT = Color.web("#FFFFFF");
    public static final Color TEXT_MUTED = Color.web("#9AA7BA");
    public static final Color TEXT_DIM = Color.web("#7F8A9C");
    public static final Color CYAN = Color.web("#00E5FF");
    public static final Color CYAN_DARK = Color.web("#00B4D8");
    public static final Color YELLOW = Color.web("#FFD600");
    public static final Color ORANGE = Color.web("#FF6D00");
    public static final Color RED = Color.web("#FF1E1E");
    public static final Color LIME = Color.web("#A8FF3E");
    public static final Color GREEN = Color.web("#43E51D");
    public static final Color VIOLET = Color.web("#9B4DFF");

    private Palette() {
    }

    public static Color withOpacity(Color color, double opacity) {
        double normalizedOpacity = Math.max(0.0, Math.min(1.0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), normalizedOpacity);
    }
}
