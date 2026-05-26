package fr.alescis.aelia.ui.components;

import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

/**
 * Rounded card with a resizable clip that keeps the mockup rendering stable.
 */
public class CardPane extends Pane {
    private final Rectangle clipShape;

    @SuppressWarnings("this-escape")
    public CardPane(double width, double height) {
        setPrefSize(width, height);
        setMinSize(width, height);
        setMaxSize(width, height);
        clipShape = new Rectangle(width, height);
        clipShape.setArcWidth(36.0);
        clipShape.setArcHeight(36.0);
        clipShape.widthProperty().bind(widthProperty());
        clipShape.heightProperty().bind(heightProperty());
        setClip(clipShape);
        getStyleClass().add("aelia-card");
    }
}
