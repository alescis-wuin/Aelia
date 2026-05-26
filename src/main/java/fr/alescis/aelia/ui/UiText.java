package fr.alescis.aelia.ui;

import javafx.scene.control.Label;

/**
 * Factory methods for text nodes using the CSS design system.
 */
public final class UiText {
    private UiText() {
    }

    public static Label label(String text, String styleClass) {
        Label label = new Label(text == null ? "" : text);
        if (styleClass != null && !styleClass.isBlank()) {
            label.getStyleClass().add(styleClass);
        }
        label.setMouseTransparent(true);
        return label;
    }

    public static Label brand(String text) {
        return label(UiFormatters.trackedUppercase(text), "brand-text");
    }

    public static Label section(String text) {
        return label(UiFormatters.trackedUppercase(text), "section-title");
    }

    public static Label data(String text, String styleClass) {
        return label(text, styleClass);
    }
}
