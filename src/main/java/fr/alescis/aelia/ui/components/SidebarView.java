package fr.alescis.aelia.ui.components;

import fr.alescis.aelia.model.LocationWeather;
import fr.alescis.aelia.ui.AccessibilitySupport;
import fr.alescis.aelia.ui.Palette;
import fr.alescis.aelia.ui.UiText;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.util.ArrayList;
import java.util.List;

/**
 * Left navigation rail with brand, search, favorite locations and bottom navigation.
 */
public final class SidebarView extends Pane {
    private final List<Pane> locationCards = new ArrayList<>();
    private int activeLocationIndex;

    public SidebarView(List<LocationWeather> locations) {
        setPrefSize(264, 854);
        getStyleClass().add("sidebar");
        buildBrand();
        buildSearchField();
        buildLocations(locations);
        buildBottomNavigation();
        AccessibilitySupport.describe(this, AccessibleRole.PARENT, "Aelia navigation sidebar", "Contains city search, saved locations and main navigation.");
    }

    private void buildBrand() {
        Node logo = WeatherIcons.logoCloud();
        logo.setLayoutX(0);
        logo.setLayoutY(0);
        Group logoWrapper = new Group(logo);
        logoWrapper.setLayoutX(0);
        logoWrapper.setLayoutY(0);
        Label brand = UiText.brand("Aelia");
        brand.setLayoutX(60);
        brand.setLayoutY(11);
        getChildren().addAll(logoWrapper, brand);
    }

    private void buildSearchField() {
        TextField field = new TextField();
        field.setPromptText("Rechercher une ville…");
        field.getStyleClass().add("city-search-field");
        field.setLayoutX(14);
        field.setLayoutY(42);
        field.setPrefSize(236, 34);
        field.setAccessibleText("Rechercher une ville");
        field.setAccessibleHelp("Saisir une ville pour une future recherche météo.");

        Node search = WeatherIcons.searchIcon();
        search.setLayoutX(33);
        search.setLayoutY(59);
        search.setMouseTransparent(true);

        getChildren().addAll(field, search);
    }

    private void buildLocations(List<LocationWeather> locations) {
        Label title = UiText.section("Mes lieux");
        title.setLayoutX(14);
        title.setLayoutY(90);
        getChildren().add(title);

        for (int index = 0; index < locations.size(); index++) {
            LocationWeather location = locations.get(index);
            Pane card = locationCard(location, index);
            card.setLayoutX(8);
            card.setLayoutY(104 + index * 61.0);
            locationCards.add(card);
            getChildren().add(card);
            if (location.selected()) {
                activeLocationIndex = index;
            }
        }
    }

    private Pane locationCard(LocationWeather location, int index) {
        Pane card = new Pane();
        card.setPrefSize(248, 55);
        card.getStyleClass().add(location.selected() ? "location-card-active" : "location-card");
        card.setFocusTraversable(true);
        card.setAccessibleRole(AccessibleRole.BUTTON);
        card.setAccessibleText(location.city() + ", " + location.country() + ", " + location.condition().label() + ", " + location.temperatureCelsius() + " degrés");
        card.setAccessibleHelp("Sélectionne cette zone géographique.");

        Circle marker = new Circle(5);
        marker.setLayoutX(19);
        marker.setLayoutY(27);
        marker.setFill(location.selected() ? Palette.CYAN : Palette.withOpacity(Palette.TEXT, 0.36));

        Label city = UiText.label(location.city(), location.selected() ? "location-city-active" : "location-city");
        city.setLayoutX(40);
        city.setLayoutY(11);

        Label details = UiText.label(location.country() + " · " + location.condition().label(), "location-details");
        details.setLayoutX(40);
        details.setLayoutY(29);

        Label temperature = UiText.data(location.temperatureCelsius() + "°", location.selected() ? "location-temp-active" : "location-temp");
        temperature.setLayoutX(212);
        temperature.setLayoutY(10);
        temperature.setAlignment(Pos.CENTER_RIGHT);
        temperature.setPrefWidth(30);

        card.getChildren().addAll(marker, city, details, temperature);
        card.setOnMouseClicked(event -> selectLocation(index));
        card.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                selectLocation(index);
                event.consume();
            }
        });
        return card;
    }

    private void selectLocation(int index) {
        activeLocationIndex = index;
        for (int i = 0; i < locationCards.size(); i++) {
            Pane card = locationCards.get(i);
            card.getStyleClass().removeAll("location-card-active", "location-card");
            card.getStyleClass().add(i == activeLocationIndex ? "location-card-active" : "location-card");
        }
    }

    private void buildBottomNavigation() {
        Line separator = new Line(14, 800, 250, 800);
        separator.setStroke(Palette.BORDER);
        separator.setStrokeWidth(1.0);
        getChildren().add(separator);

        getChildren().add(bottomNavItem(54, WeatherIcons.homeIcon(), "ACCUEIL", true));
        getChildren().add(bottomNavItem(126, WeatherIcons.mapIcon(), "CARTE", false));
        getChildren().add(bottomNavItem(200, WeatherIcons.gearIcon(), "RÉGLAGES", false));
    }

    private Node bottomNavItem(double centerX, Node icon, String label, boolean active) {
        Pane pane = new Pane();
        pane.setPrefSize(64, 48);
        pane.setLayoutX(centerX - 32);
        pane.setLayoutY(804);
        pane.setFocusTraversable(true);
        pane.setAccessibleRole(AccessibleRole.BUTTON);
        pane.setAccessibleText(label.toLowerCase());
        pane.setAccessibleHelp("Navigation principale : " + label.toLowerCase());

        StackPane iconSlot = new StackPane(icon);
        iconSlot.setLayoutX(0);
        iconSlot.setLayoutY(7);
        iconSlot.setPrefSize(64, 26);
        iconSlot.setMinSize(64, 26);
        iconSlot.setMaxSize(64, 26);
        iconSlot.setAlignment(Pos.CENTER);
        iconSlot.setMouseTransparent(true);

        Label text = UiText.label(label, active ? "nav-label-active" : "nav-label");
        text.setAlignment(Pos.CENTER);
        text.setPrefWidth(64);
        text.setLayoutX(0);
        text.setLayoutY(37);
        pane.getChildren().addAll(iconSlot, text);
        return pane;
    }
}
