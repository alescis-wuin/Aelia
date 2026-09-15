package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.PollenLevel;
import fr.alescis.aelia.model.PollenRisk;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pollen risk card with per-type bars and a compact legend.
 */
public final class PollenCard extends CardPane {
    public PollenCard(List<PollenRisk> risks) {
        super(254, 254);
        Label title = UiText.section("Pollens");
        title.setLayoutX(20);
        title.setLayoutY(19);
        getChildren().add(title);

        for (int index = 0; index < risks.size(); index++) {
            addRisk(risks.get(index), 36 + index * 44.0);
        }
        addLegend();
        String accessible = risks.stream()
                .map(risk -> risk.name() + " " + risk.level().label())
                .collect(Collectors.joining(", "));
        TooltipSupport.install(this, "Niveaux de pollen : " + accessible);
        AccessibilitySupport.describe(this, AccessibleRole.TEXT, "Niveaux de pollen : " + accessible,
                "Carte des risques polliniques par type de pollen.");
    }

    private void addRisk(PollenRisk risk, double y) {
        Rectangle row = new Rectangle(222, 38);
        row.setLayoutX(16);
        row.setLayoutY(y);
        row.setArcWidth(8);
        row.setArcHeight(8);
        row.getStyleClass().add("pollen-row");

        Color color = colorFor(risk.level());
        Circle bullet = new Circle(20, y + 19, 4, color);
        bullet.setOpacity(risk.level() == PollenLevel.NONE ? 0.45 : 0.9);

        Label name = UiText.label(risk.name(), "pollen-name");
        name.setLayoutX(34);
        name.setLayoutY(y + 9);

        Rectangle track = new Rectangle(130, 5);
        track.setLayoutX(34);
        track.setLayoutY(y + 25);
        track.setArcWidth(5);
        track.setArcHeight(5);
        track.getStyleClass().add("pollen-track");

        Rectangle progress = new Rectangle(130 * risk.level().normalizedValue(), 5);
        progress.setLayoutX(34);
        progress.setLayoutY(y + 25);
        progress.setArcWidth(5);
        progress.setArcHeight(5);
        progress.setFill(color);
        progress.setOpacity(risk.level() == PollenLevel.NONE ? 0.35 : 0.86);

        Label level = UiText.label(risk.level().label(), "pollen-level");
        level.setTextFill(color);
        if (risk.level() == PollenLevel.NONE) {
            level.setOpacity(0.45);
        }
        level.setLayoutX(174);
        level.setLayoutY(y + 9);
        level.setPrefWidth(60);

        String tooltipText = risk.name() + " : " + risk.level().label();
        TooltipSupport.install(row, tooltipText);
        getChildren().addAll(row, bullet, name, track, progress, level);
    }

    private void addLegend() {
        Rectangle background = new Rectangle(222, 24);
        background.setLayoutX(16);
        background.setLayoutY(212);
        background.setArcWidth(8);
        background.setArcHeight(8);
        background.getStyleClass().add("pollen-legend");
        getChildren().add(background);

        addLegendItem(28, "Nul", Palette.withOpacity(Palette.TEXT, 0.35));
        addLegendItem(70, "Faible", Palette.LIME);
        addLegendItem(114, "Moyen", Palette.YELLOW);
        addLegendItem(164, "Fort", Palette.ORANGE);
    }

    private void addLegendItem(double x, String label, Color color) {
        Circle bullet = new Circle(x, 224, 3.5, color);
        Label text = UiText.label(label, "legend-small");
        text.setTextFill(color);
        text.setLayoutX(x + 8);
        text.setLayoutY(218);
        getChildren().addAll(bullet, text);
    }

    private Color colorFor(PollenLevel level) {
        return switch (level) {
            case NONE -> Palette.withOpacity(Palette.TEXT, 0.35);
            case LOW -> Palette.LIME;
            case MODERATE -> Palette.YELLOW;
            case HIGH -> Palette.ORANGE;
            case EXTREME -> Palette.RED;
        };
    }
}
