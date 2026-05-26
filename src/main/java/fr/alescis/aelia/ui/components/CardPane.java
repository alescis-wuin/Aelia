package fr.alescis.aelia.ui.components;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

/**
 * Flat rounded card with fixed design-system styling and clipped content.
 */
public class CardPane extends Pane {
    public CardPane(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(36.0);
        clip.setArcHeight(36.0);
        setClip(clip);
        getStyleClass().add("aelia-card");
    }
}
