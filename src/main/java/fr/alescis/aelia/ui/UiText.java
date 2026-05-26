package fr.alescis.aelia.ui;

import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * Factory for consistently styled labels.
 */
public final class UiText {
    private UiText() {
    }

    public static Label brand(String text) {
        Label label = new Label(spaced(text.toUpperCase(Locale.ROOT)));
        label.getStyleClass().add("brand-text");
        return label;
    }

    public static Label section(String text) {
        Label label = new Label(spaced(text.toUpperCase(Locale.ROOT)));
        label.getStyleClass().add("section-title");
        return label;
    }

    public static Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    public static Label data(String text, String styleClass) {
        Label label = label(text, styleClass);
        label.getStyleClass().add("data-font");
        return label;
    }

    public static void color(Label label, Color color) {
        label.setTextFill(color);
    }

    public static void accessible(Label label, String text) {
        label.setAccessibleRole(AccessibleRole.TEXT);
        label.setAccessibleText(text);
    }

    private static String spaced(String value) {
        return String.join(" ", value.split(""));
    }
}
