package fr.alescis.aelia.ui;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.ui.components.MetricTile;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Programmatic JavaFX view for the first UI/UX-focused Aelia dashboard.
 */
public class DashboardView {
    private final BorderPane root = new BorderPane();
    private final Label providerStatusLabel = new Label("Local simulator");
    private final Label lastStatusLabel = new Label("Ready");
    private final Label heroMetricLabel = new Label("Air temperature");
    private final Label heroValueLabel = new Label("—");
    private final Label heroDescriptionLabel = new Label("Select a metric, request the current value, or start a live stream.");
    private final Label heroTimestampLabel = new Label("No value yet");
    private final Label conditionBadge = new Label("Condition —");
    private final HBox glanceRow = new HBox(12);
    private final ComboBox<DataMetric> metricSelector = new ComboBox<>();
    private final Spinner<Integer> intervalSpinner = new Spinner<>(1, 3600, 5);
    private final Button readNowButton = new Button("_Read now");
    private final Button subscribeButton = new Button("_Subscribe");
    private final Button unsubscribeButton = new Button("_Stop selected");
    private final TableView<SubscriptionViewItem> subscriptionTable = new TableView<>();
    private final TableView<DataMetric> metricsTable = new TableView<>();
    private final TableView<ApiLimit> limitsTable = new TableView<>();
    private final Map<String, MetricTile> glanceTiles = new LinkedHashMap<>();

    public DashboardView() {
        buildLayout();
        configureAccessibility();
    }

    public Parent root() {
        return root;
    }

    public Label providerStatusLabel() {
        return providerStatusLabel;
    }

    public Label lastStatusLabel() {
        return lastStatusLabel;
    }

    public Label heroMetricLabel() {
        return heroMetricLabel;
    }

    public Label heroValueLabel() {
        return heroValueLabel;
    }

    public Label heroDescriptionLabel() {
        return heroDescriptionLabel;
    }

    public Label heroTimestampLabel() {
        return heroTimestampLabel;
    }

    public Label conditionBadge() {
        return conditionBadge;
    }

    public ComboBox<DataMetric> metricSelector() {
        return metricSelector;
    }

    public Spinner<Integer> intervalSpinner() {
        return intervalSpinner;
    }

    public Button readNowButton() {
        return readNowButton;
    }

    public Button subscribeButton() {
        return subscribeButton;
    }

    public Button unsubscribeButton() {
        return unsubscribeButton;
    }

    public TableView<SubscriptionViewItem> subscriptionTable() {
        return subscriptionTable;
    }

    public TableView<DataMetric> metricsTable() {
        return metricsTable;
    }

    public TableView<ApiLimit> limitsTable() {
        return limitsTable;
    }

    public Map<String, MetricTile> glanceTiles() {
        return Map.copyOf(glanceTiles);
    }

    public void createGlanceTiles(List<DataMetric> metrics) {
        glanceTiles.clear();
        glanceRow.getChildren().clear();
        for (DataMetric metric : metrics) {
            MetricTile tile = new MetricTile(metric);
            glanceTiles.put(metric.id(), tile);
            HBox.setHgrow(tile, Priority.ALWAYS);
            tile.setMaxWidth(Double.MAX_VALUE);
            glanceRow.getChildren().add(tile);
        }
    }

    public void updateGlanceTile(MetricValue value) {
        MetricTile tile = glanceTiles.get(value.metric().id());
        if (tile != null) {
            tile.update(value);
        }
    }

    private void buildLayout() {
        root.getStyleClass().add("app-root");
        root.setTop(buildHeader());
        root.setCenter(buildTabs());
    }

    private VBox buildHeader() {
        Label title = new Label("Aelia");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Focused environmental values. Local simulation. Ready for remote providers.");
        subtitle.getStyleClass().add("app-subtitle");

        providerStatusLabel.getStyleClass().addAll("pill", "pill-info");
        lastStatusLabel.getStyleClass().addAll("pill", "pill-ok");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerLine = new HBox(12, new VBox(2, title, subtitle), spacer, providerStatusLabel, lastStatusLabel);
        headerLine.setAlignment(Pos.CENTER_LEFT);
        headerLine.getStyleClass().add("header-line");

        VBox header = new VBox(headerLine);
        header.getStyleClass().add("app-header");
        return header;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("main-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab liveTab = new Tab("Live", buildLiveTab());
        liveTab.setTooltip(new Tooltip("Current values and live subscriptions."));
        Tab referenceTab = new Tab("Reference", buildReferenceTab());
        referenceTab.setTooltip(new Tooltip("Supported data and simulated API limits."));
        tabs.getTabs().addAll(liveTab, referenceTab);
        return tabs;
    }

    private VBox buildLiveTab() {
        VBox content = new VBox(18, buildHeroCard(), buildActionsCard(), buildStreamsCard());
        content.getStyleClass().add("tab-content");
        VBox.setVgrow(subscriptionTable, Priority.ALWAYS);
        return content;
    }

    private VBox buildHeroCard() {
        conditionBadge.getStyleClass().add("condition-badge");
        heroMetricLabel.getStyleClass().add("hero-title");
        heroValueLabel.getStyleClass().add("hero-value");
        heroDescriptionLabel.getStyleClass().add("hero-description");
        heroDescriptionLabel.setWrapText(true);
        heroTimestampLabel.getStyleClass().add("hero-meta");

        VBox valueStack = new VBox(8, conditionBadge, heroMetricLabel, heroValueLabel, heroDescriptionLabel, heroTimestampLabel);
        valueStack.getStyleClass().add("hero-copy");

        glanceRow.setId("glanceRow");
        glanceRow.getStyleClass().add("glance-row");
        VBox.setVgrow(glanceRow, Priority.NEVER);

        VBox hero = new VBox(18, valueStack, glanceRow);
        hero.getStyleClass().addAll("card", "hero-card");
        return hero;
    }

    private VBox buildActionsCard() {
        Label cardTitle = sectionTitle("Query and stream");
        Label metricLabel = formLabel("Metric", metricSelector);
        Label intervalLabel = formLabel("Interval", intervalSpinner);

        configureMetricSelector();
        configureButtons();

        GridPane grid = new GridPane();
        grid.getStyleClass().add("form-grid");
        grid.setHgap(12);
        grid.setVgap(8);
        grid.add(metricLabel, 0, 0);
        grid.add(intervalLabel, 1, 0);
        grid.add(metricSelector, 0, 1);
        grid.add(intervalSpinner, 1, 1);
        grid.add(readNowButton, 2, 1);
        grid.add(subscribeButton, 3, 1);
        GridPane.setHgrow(metricSelector, Priority.ALWAYS);

        VBox card = new VBox(12, cardTitle, grid);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildStreamsCard() {
        Label cardTitle = sectionTitle("Active streams");
        configureSubscriptionTable();

        HBox titleRow = new HBox(12, cardTitle, unsubscribeButton);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cardTitle, Priority.ALWAYS);

        VBox card = new VBox(12, titleRow, subscriptionTable);
        card.getStyleClass().add("card");
        VBox.setVgrow(subscriptionTable, Priority.ALWAYS);
        return card;
    }

    private VBox buildReferenceTab() {
        configureMetricsTable();
        configureLimitsTable();

        VBox metricsCard = new VBox(12, sectionTitle("Supported data"), metricsTable);
        metricsCard.getStyleClass().add("card");
        VBox.setVgrow(metricsTable, Priority.ALWAYS);

        VBox limitsCard = new VBox(12, sectionTitle("Provider limits"), limitsTable);
        limitsCard.getStyleClass().add("card");
        VBox.setVgrow(limitsTable, Priority.ALWAYS);

        VBox content = new VBox(18, metricsCard, limitsCard);
        content.getStyleClass().add("tab-content");
        VBox.setVgrow(metricsCard, Priority.ALWAYS);
        VBox.setVgrow(limitsCard, Priority.ALWAYS);
        return content;
    }

    private void configureMetricSelector() {
        metricSelector.setMaxWidth(Double.MAX_VALUE);
        metricSelector.setTooltip(new Tooltip("Choose the data to read or stream. Keyboard: Ctrl+L."));
        metricSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(DataMetric metric) {
                return metric == null ? "" : metric.displayName();
            }

            @Override
            public DataMetric fromString(String value) {
                return metricSelector.getItems().stream()
                        .filter(metric -> metric.displayName().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        metricSelector.setCellFactory(list -> new MetricListCell());
        metricSelector.setButtonCell(new MetricListCell());

        intervalSpinner.setEditable(true);
        intervalSpinner.setMaxWidth(120.0d);
        intervalSpinner.setTooltip(new Tooltip("Subscription interval in seconds. Range: 1 to 3600."));
    }

    private void configureButtons() {
        readNowButton.setMnemonicParsing(true);
        subscribeButton.setMnemonicParsing(true);
        unsubscribeButton.setMnemonicParsing(true);
        readNowButton.getStyleClass().add("primary-button");
        subscribeButton.getStyleClass().add("secondary-button");
        unsubscribeButton.getStyleClass().add("danger-button");
        readNowButton.setTooltip(new Tooltip("Request the selected value now. Keyboard: Ctrl+R."));
        subscribeButton.setTooltip(new Tooltip("Create a periodic stream for the selected metric. Keyboard: Ctrl+S."));
        unsubscribeButton.setTooltip(new Tooltip("Stop the selected stream. Keyboard: Delete."));
    }

    private void configureSubscriptionTable() {
        subscriptionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        subscriptionTable.setPlaceholder(new Label("No active stream."));
        subscriptionTable.getStyleClass().add("compact-table");

        TableColumn<SubscriptionViewItem, String> metric = new TableColumn<>("Metric");
        metric.setCellValueFactory(value -> value.getValue().metricNameProperty());
        metric.setMinWidth(160.0d);

        TableColumn<SubscriptionViewItem, String> interval = new TableColumn<>("Every");
        interval.setCellValueFactory(value -> value.getValue().intervalTextProperty());
        interval.setMinWidth(80.0d);

        TableColumn<SubscriptionViewItem, String> value = new TableColumn<>("Last value");
        value.setCellValueFactory(item -> item.getValue().lastValueProperty());
        value.setMinWidth(140.0d);

        TableColumn<SubscriptionViewItem, String> updated = new TableColumn<>("Updated");
        updated.setCellValueFactory(item -> item.getValue().lastUpdateProperty());
        updated.setMinWidth(100.0d);

        subscriptionTable.getColumns().setAll(List.of(metric, interval, value, updated));
    }

    private void configureMetricsTable() {
        metricsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        metricsTable.setPlaceholder(new Label("No supported metric."));

        TableColumn<DataMetric, String> metric = new TableColumn<>("Metric");
        metric.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().displayName()));
        metric.setMinWidth(160.0d);

        TableColumn<DataMetric, String> id = new TableColumn<>("Id");
        id.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().id()));
        id.setMinWidth(150.0d);

        TableColumn<DataMetric, String> group = new TableColumn<>("Group");
        group.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().category().label()));
        group.setMinWidth(100.0d);

        TableColumn<DataMetric, String> range = new TableColumn<>("Range");
        range.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().rangeLabel()));
        range.setMinWidth(110.0d);

        TableColumn<DataMetric, String> description = new TableColumn<>("Description");
        description.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().description()));
        description.setMinWidth(320.0d);

        metricsTable.getColumns().setAll(List.of(metric, id, group, range, description));
    }

    private void configureLimitsTable() {
        limitsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        limitsTable.setPlaceholder(new Label("No limit."));

        TableColumn<ApiLimit, String> resource = new TableColumn<>("Resource");
        resource.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().value()));
        resource.setMinWidth(180.0d);

        TableColumn<ApiLimit, String> period = new TableColumn<>("Period");
        period.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().period()));
        period.setMinWidth(100.0d);

        TableColumn<ApiLimit, String> maximum = new TableColumn<>("Limit");
        maximum.setCellValueFactory(value -> new ReadOnlyStringWrapper(
                value.getValue().value() != null
                        ? value.getValue().value()
                        : "unmetered"
        ));
        maximum.setMinWidth(110.0d);

        TableColumn<ApiLimit, String> note = new TableColumn<>("Note");
        note.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().value()));
        note.setMinWidth(320.0d);

        limitsTable.getColumns().setAll(List.of(resource, period, maximum, note));
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Label formLabel(String text, javafx.scene.Node target) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        label.setLabelFor(target);
        return label;
    }

    private void configureAccessibility() {
        root.setAccessibleRole(AccessibleRole.PARENT);
        root.setAccessibleText("Aelia weather dashboard");
        providerStatusLabel.setAccessibleText("Current data provider status");
        lastStatusLabel.setAccessibleText("Last operation status");
        heroValueLabel.setAccessibleRole(AccessibleRole.TEXT);
        heroDescriptionLabel.setAccessibleRole(AccessibleRole.TEXT);
        metricSelector.setAccessibleText("Metric selector");
        intervalSpinner.setAccessibleText("Stream interval in seconds");
        subscriptionTable.setAccessibleRole(AccessibleRole.TABLE_VIEW);
        subscriptionTable.setAccessibleText("Active streams table");
        metricsTable.setAccessibleRole(AccessibleRole.TABLE_VIEW);
        metricsTable.setAccessibleText("Supported data table");
        limitsTable.setAccessibleRole(AccessibleRole.TABLE_VIEW);
        limitsTable.setAccessibleText("Provider limits table");
        BorderPane.setMargin(root.getCenter(), new Insets(0));
    }
}
