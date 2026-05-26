package fr.alescis.aelia.ui;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;

/**
 * Small utility for applying accessible metadata to non-standard visual components.
 */
public final class AccessibilitySupport {
    private AccessibilitySupport() {
    }

    public static void describe(Node node, AccessibleRole role, String text, String help) {
        node.setAccessibleRole(role);
        node.setAccessibleText(text);
        node.setAccessibleHelp(help);
        node.setFocusTraversable(true);
        if (help != null && !help.isBlank()) {
            Tooltip.install(node, new Tooltip(help));
        }
    }
}
