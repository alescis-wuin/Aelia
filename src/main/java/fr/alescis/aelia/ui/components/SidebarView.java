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
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Left navigation rail with brand, local search, saved locations and bottom navigation.
 */
public final class SidebarView extends Pane {
    private static final double FIRST_LOCATION_Y = 104.0;
    private static final double LOCATION_SPACING = 61.0;

    private final List<LocationCardView> locationCards = new ArrayList<>();
    private final List<NavigationItemView> navigationItems = new ArrayList<>();
    private final IntConsumer locationSelectionHandler;
    private final Consumer<String> navigationSelectionHandler;
    private Label emptySearchResult;
    private int activeLocationIndex;
    private String activeNavigationId = "home";

    public SidebarView(List<LocationWeather> locations) {
        this(locations, index -> { }, id -> { });
    }

    public SidebarView(List<LocationWeather> locations, IntConsumer locationSelectionHandler, Consumer<String> navigationSelectionHandler) {
        this.locationSelectionHandler = Objects.requireNonNull(locationSelectionHandler, "locationSelectionHandler");
        this.navigationSelectionHandler = Objects.requireNonNull(navigationSelectionHandler, "navigationSelectionHandler");
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
        field.setAccessibleHelp("Filtre les lieux enregistrés. Entrée sélectionne le premier résultat.");
        field.textProperty().addListener((observable, oldValue, newValue) -> filterLocations(newValue));
        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                selectFirstVisibleLocation();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                field.clear();
                event.consume();
            }
        });
        TooltipSupport.install(field, "Recherche locale parmi les lieux enregistrés. Entrée sélectionne le premier résultat, Échap efface la recherche.");

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

        emptySearchResult = UiText.label("Aucun lieu enregistré", "location-details");
        emptySearchResult.setLayoutX(40);
        emptySearchResult.setLayoutY(FIRST_LOCATION_Y + 16);
        emptySearchResult.setVisible(false);
        getChildren().add(emptySearchResult);

        for (int index = 0; index < locations.size(); index++) {
            LocationWeather location = locations.get(index);
            Pane card = locationCard(location, index);
            card.setLayoutX(8);
            card.setLayoutY(FIRST_LOCATION_Y + index * LOCATION_SPACING);
            locationCards.add(new LocationCardView(card, location));
            getChildren().add(card);
            if (location.selected()) {
                activeLocationIndex = index;
            }
        }
        updateLocationSelection();
    }

    private Pane locationCard(LocationWeather location, int index) {
        Pane card = new Pane();
        card.setPrefSize(248, 55);
        card.setFocusTraversable(true);
        card.setAccessibleRole(AccessibleRole.BUTTON);
        card.setAccessibleText(location.city() + ", " + location.country() + ", " + location.condition().label() + ", " + location.temperatureCelsius() + " degrés");
        card.setAccessibleHelp("Sélectionne cette zone géographique.");

        Circle marker = new Circle(5);
        marker.setLayoutX(19);
        marker.setLayoutY(27);

        Label city = UiText.label(location.city(), "location-city");
        city.setLayoutX(40);
        city.setLayoutY(11);

        Label details = UiText.label(location.country() + " · " + location.condition().label(), "location-details");
        details.setLayoutX(40);
        details.setLayoutY(29);

        Label temperature = UiText.data(location.temperatureCelsius() + "°", "location-temp");
        temperature.setLayoutX(212);
        temperature.setLayoutY(10);
        temperature.setAlignment(Pos.CENTER_RIGHT);
        temperature.setPrefWidth(30);

        card.getChildren().addAll(marker, city, details, temperature);
        card.setOnMouseClicked(event -> {
            if (!event.isStillSincePress()) {
                return;
            }
            selectLocation(index, true);
            event.consume();
        });
        card.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                selectLocation(index, true);
                event.consume();
            }
        });
        TooltipSupport.install(card, location.city() + " · " + location.country() + " · " + location.condition().label() + " · " + location.temperatureCelsius() + " °C");
        return card;
    }

    private void selectLocation(int index, boolean notify) {
        if (index < 0 || index >= locationCards.size()) {
            return;
        }
        activeLocationIndex = index;
        updateLocationSelection();
        if (notify) {
            locationSelectionHandler.accept(index);
        }
    }

    private void updateLocationSelection() {
        for (int i = 0; i < locationCards.size(); i++) {
            LocationCardView view = locationCards.get(i);
            boolean selected = i == activeLocationIndex;
            view.pane().getStyleClass().removeAll("location-card-active", "location-card");
            view.pane().getStyleClass().add(selected ? "location-card-active" : "location-card");
            updateCardLabels(view.pane(), selected);
        }
    }

    private void updateCardLabels(Pane card, boolean selected) {
        for (Node child : card.getChildren()) {
            if (child instanceof Circle circle) {
                circle.setFill(selected ? Palette.CYAN : Palette.withOpacity(Palette.TEXT, 0.36));
            } else if (child instanceof Label label) {
                label.getStyleClass().removeAll("location-city-active", "location-city", "location-temp-active", "location-temp");
                if (label.getText().endsWith("°")) {
                    label.getStyleClass().add(selected ? "location-temp-active" : "location-temp");
                } else if (!label.getText().contains(" · ")) {
                    label.getStyleClass().add(selected ? "location-city-active" : "location-city");
                }
            }
        }
    }

    private void filterLocations(String rawQuery) {
        String query = normalize(rawQuery);
        int visibleIndex = 0;
        for (LocationCardView view : locationCards) {
            boolean visible = query.isBlank() || normalize(view.location().city()).contains(query) || normalize(view.location().country()).contains(query);
            view.pane().setVisible(visible);
            if (visible) {
                view.pane().setLayoutY(FIRST_LOCATION_Y + visibleIndex * LOCATION_SPACING);
                visibleIndex++;
            }
        }
        emptySearchResult.setVisible(visibleIndex == 0);
    }

    private void selectFirstVisibleLocation() {
        for (int i = 0; i < locationCards.size(); i++) {
            if (locationCards.get(i).pane().isVisible()) {
                selectLocation(i, true);
                return;
            }
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "");
    }

    private void buildBottomNavigation() {
        Line separator = new Line(14, 800, 250, 800);
        separator.setStroke(Palette.BORDER);
        separator.setStrokeWidth(1.0);
        getChildren().add(separator);

        addNavigationItem(54, WeatherIcons.homeIcon(), "ACCUEIL", "home");
        addNavigationItem(126, WeatherIcons.mapIcon(), "CARTE", "map");
        addNavigationItem(200, WeatherIcons.gearIcon(), "RÉGLAGES", "settings");
        updateNavigationSelection();
    }

    private void addNavigationItem(double centerX, Node icon, String label, String id) {
        Pane pane = new Pane();
        pane.setPrefSize(64, 48);
        pane.setLayoutX(centerX - 32);
        pane.setLayoutY(804);
        pane.setFocusTraversable(true);
        pane.setAccessibleRole(AccessibleRole.BUTTON);
        pane.setAccessibleText(label.toLowerCase(Locale.ROOT));
        pane.setAccessibleHelp("Navigation principale : " + label.toLowerCase(Locale.ROOT));

        StackPane iconSlot = new StackPane(icon);
        iconSlot.setLayoutX(0);
        iconSlot.setLayoutY(7);
        iconSlot.setPrefSize(64, 26);
        iconSlot.setMinSize(64, 26);
        iconSlot.setMaxSize(64, 26);
        iconSlot.setAlignment(Pos.CENTER);
        iconSlot.setMouseTransparent(true);

        Label text = UiText.label(label, "nav-label");
        text.setAlignment(Pos.CENTER);
        text.setPrefWidth(64);
        text.setLayoutX(0);
        text.setLayoutY(37);
        pane.getChildren().addAll(iconSlot, text);
        pane.setOnMouseClicked(event -> {
            if (!event.isStillSincePress()) {
                return;
            }
            selectNavigation(id);
            event.consume();
        });
        pane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                selectNavigation(id);
                event.consume();
            }
        });
        TooltipSupport.install(pane, "Ouvrir l'onglet " + label.toLowerCase(Locale.FRANCE));
        navigationItems.add(new NavigationItemView(id, pane, icon, text));
        getChildren().add(pane);
    }

    private void selectNavigation(String id) {
        activeNavigationId = Objects.requireNonNull(id, "id");
        updateNavigationSelection();
        navigationSelectionHandler.accept(id);
    }

    private void updateNavigationSelection() {
        for (NavigationItemView item : navigationItems) {
            boolean active = item.id().equals(activeNavigationId);
            item.label().getStyleClass().removeAll("nav-label-active", "nav-label");
            item.label().getStyleClass().add(active ? "nav-label-active" : "nav-label");
            item.icon().setOpacity(active ? 1.0 : 0.75);
        }
    }

    private record LocationCardView(Pane pane, LocationWeather location) {
    }

    private record NavigationItemView(String id, Pane pane, Node icon, Label label) {
    }
}
