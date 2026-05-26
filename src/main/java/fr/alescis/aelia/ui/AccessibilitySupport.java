package fr.alescis.aelia.ui;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;

/**
 * Small helper to keep accessibility assignments consistent.
 */
public final class AccessibilitySupport {
    private AccessibilitySupport() {
    }

    public static void describe(Node node, AccessibleRole role, String text, String help) {
        node.setAccessibleRole(role);
        node.setAccessibleText(text);
        node.setAccessibleHelp(help);
    }
}
