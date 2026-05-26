package fr.alescis.aelia.ui;

import javafx.scene.paint.Color;

/**
 * Shared flat color palette used by the JavaFX vector components.
 */
public final class Palette {
    public static final Color BACKGROUND = Color.web("#070B14");
    public static final Color SIDEBAR = Color.web("#0B1220");
    public static final Color CARD = Color.web("#0E1724");
    public static final Color CARD_ELEVATED = Color.web("#131E32");
    public static final Color BORDER = Color.web("#1E2E48");
    public static final Color BORDER_SOFT = Color.web("#1E2A3A");
    public static final Color TEXT = Color.web("#FFFFFF");
    public static final Color TEXT_MUTED = Color.web("#9AA7BA");
    public static final Color CYAN = Color.web("#00E5FF");
    public static final Color CYAN_DARK = Color.web("#00B4D8");
    public static final Color YELLOW = Color.web("#FFD600");
    public static final Color LIME = Color.web("#A8FF3E");
    public static final Color GREEN = Color.web("#57CC00");
    public static final Color ORANGE = Color.web("#FF6D00");
    public static final Color RED = Color.web("#D50000");
    public static final Color VIOLET = Color.web("#9B2FFF");
    public static final Color CLOUD = Color.web("#7090B0");
    public static final Color CLOUD_DARK = Color.web("#6080A0");

    private Palette() {
    }

    public static Color withOpacity(Color color, double opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
    }
}
