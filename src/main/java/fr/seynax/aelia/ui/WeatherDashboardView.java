package fr.seynax.aelia.ui;

import fr.seynax.aelia.model.ApiLimit;
import fr.seynax.aelia.model.DataMetric;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * JavaFX view for the minimal weather utility dashboard.
 */
public final class WeatherDashboardView {

    private final BorderPane root = new BorderPane();
    private final Label providerLabel = new Label("Provider");
    private final Label accessLabel = new Label("Access");
    private final Label statusLabel = new Label("Ready");
    private final TextField searchField = new TextField();
    private final ListView<DataMetric> metricList = new ListView<>();
    private final TableView<DataMetric> metricsTable = new TableView<>();
    private final TableView<ApiLimit> limitsTable = new TableView<>();
    private final Label currentMetricLabel = new Label("Select a metric");
    private final Label currentValueLabel = new Label("—");
    private final Label currentDescriptionLabel = new Label("Choose a data point to request its current value.");
    private final Label currentTimestampLabel = new Label("No value yet");
    private final Button refreshButton = new Button("_Refresh current value");
    private final ComboBox<DataMetric> subscriptionMetricBox = new ComboBox<>();
    private final Spinner<Integer> intervalSecondsSpinner = new Spinner<>(1, 3600, 5);
    private final Button subscribeButton = new Button("_Subscribe");
    private final Button unsubscribeButton = new Button("_Unsubscribe selected");
    private final TableView<SubscriptionViewItem> subscriptionTable = new TableView<>();
    private final ListView<String> updateFeed = new ListView<>();

    public WeatherDashboardView() {
        buildLayout();
        configureAccessibility();
    }

    public Parent root() {
        return root;
    }

    public Label providerLabel() {
        return providerLabel;
    }

    public Label accessLabel() {
        return accessLabel;
    }

    public Label statusLabel() {
        return statusLabel;
    }

    public TextField searchField() {
        return searchField;
    }

    public ListView<DataMetric> metricList() {
        return metricList;
    }

    public TableView<DataMetric> metricsTable() {
        return metricsTable;
    }

    public TableView<ApiLimit> limitsTable() {
        return limitsTable;
    }

    public Label currentMetricLabel() {
        return currentMetricLabel;
    }

    public Label currentValueLabel() {
        return currentValueLabel;
    }

    public Label currentDescriptionLabel() {
        return currentDescriptionLabel;
    }

    public Label currentTimestampLabel() {
        return currentTimestampLabel;
    }

    public Button refreshButton() {
        return refreshButton;
    }

    public ComboBox<DataMetric> subscriptionMetricBox() {
        return subscriptionMetricBox;
    }

    public Spinner<Integer> intervalSecondsSpinner() {
        return intervalSecondsSpinner;
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

    public ListView<String> updateFeed() {
        return updateFeed;
    }

    private void buildLayout() {
        root.getStyleClass().add("app-root");
        root.setTop(buildHeader());

        SplitPane mainSplit = new SplitPane(buildMetricsPanel(), buildDashboardPanel(), buildProviderPanel());
        mainSplit.setOrientation(Orientation.HORIZONTAL);
        mainSplit.setDividerPositions(0.24, 0.68);
        mainSplit.getStyleClass().add("main-split");
        root.setCenter(mainSplit);
    }

    private VBox buildHeader() {
        Label title = new Label("Aelia");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Simulated data provider, current values and live streams.");
        subtitle.getStyleClass().add("app-subtitle");

        providerLabel.getStyleClass().add("pill");
        accessLabel.getStyleClass().add("pill");
        statusLabel.getStyleClass().add("status-pill");

        HBox metadata = new HBox(10, providerLabel, accessLabel, statusLabel);
        metadata.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(8, title, subtitle, metadata);
        header.getStyleClass().add("app-header");
        return header;
    }

    private VBox buildMetricsPanel() {
        Label sectionTitle = sectionTitle("Data catalog");
        searchField.setPromptText("Filter data");
        searchField.setTooltip(new Tooltip("Filter supported metrics by name, identifier or category."));
        metricList.setCellFactory(list -> new MetricListCell());
        metricList.getStyleClass().add("metric-list");
        VBox.setVgrow(metricList, Priority.ALWAYS);

        VBox panel = new VBox(12, sectionTitle, searchField, metricList);
        panel.getStyleClass().add("side-panel");
        return panel;
    }

    private VBox buildDashboardPanel() {
        VBox currentCard = card("Current value", currentValueContent());
        VBox streamCard = card("Live stream", streamContent());
        VBox feedCard = card("Updates", feedContent());
        VBox dashboard = new VBox(16, currentCard, streamCard, feedCard);
        dashboard.getStyleClass().add("dashboard-panel");
        VBox.setVgrow(feedCard, Priority.ALWAYS);
        return dashboard;
    }

    private VBox buildProviderPanel() {
        configureMetricsTable();
        configureLimitsTable();

        VBox metricsCard = card("Supported data", metricsTable);
        VBox limitsCard = card("API limits", limitsTable);
        VBox panel = new VBox(16, metricsCard, limitsCard);
        panel.getStyleClass().add("provider-panel");
        VBox.setVgrow(metricsCard, Priority.ALWAYS);
        VBox.setVgrow(limitsCard, Priority.ALWAYS);
        return panel;
    }

    private VBox currentValueContent() {
        currentMetricLabel.getStyleClass().add("metric-heading");
        currentValueLabel.getStyleClass().add("hero-value");
        currentDescriptionLabel.getStyleClass().add("muted-text");
        currentDescriptionLabel.setWrapText(true);
        currentTimestampLabel.getStyleClass().add("muted-text");
        refreshButton.setMnemonicParsing(true);
        refreshButton.setTooltip(new Tooltip("Request the current value for the selected metric."));
        return new VBox(8, currentMetricLabel, currentValueLabel, currentDescriptionLabel, currentTimestampLabel, refreshButton);
    }

    private VBox streamContent() {
        subscriptionMetricBox.setTooltip(new Tooltip("Metric to observe periodically."));
        subscriptionMetricBox.setConverter(new MetricStringConverter());
        subscriptionMetricBox.setCellFactory(list -> new MetricListCell());
        subscriptionMetricBox.setButtonCell(new MetricListCell());
        intervalSecondsSpinner.setEditable(true);
        intervalSecondsSpinner.setTooltip(new Tooltip("Subscription interval in seconds, from 1 to 3600."));
        subscribeButton.setMnemonicParsing(true);
        unsubscribeButton.setMnemonicParsing(true);
        subscribeButton.setTooltip(new Tooltip("Start a periodic subscription."));
        unsubscribeButton.setTooltip(new Tooltip("Cancel the selected subscription."));

        HBox controls = new HBox(10, subscriptionMetricBox, intervalSecondsSpinner, subscribeButton, unsubscribeButton);
        controls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(subscriptionMetricBox, Priority.ALWAYS);

        configureSubscriptionTable();
        VBox.setVgrow(subscriptionTable, Priority.ALWAYS);
        return new VBox(12, controls, subscriptionTable);
    }

    private VBox feedContent() {
        updateFeed.getStyleClass().add("feed-list");
        VBox.setVgrow(updateFeed, Priority.ALWAYS);
        return new VBox(updateFeed);
    }

    private VBox card(String titleText, javafx.scene.Node content) {
        Label title = sectionTitle(titleText);
        VBox card = new VBox(10, title, content);
        card.getStyleClass().add("card");
        VBox.setVgrow(content, Priority.ALWAYS);
        return card;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private void configureMetricsTable() {
        TableColumn<DataMetric, String> name = new TableColumn<>("Metric");
        name.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().displayName()));
        name.setPrefWidth(150);

        TableColumn<DataMetric, String> category = new TableColumn<>("Group");
        category.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().category().label()));
        category.setPrefWidth(95);

        TableColumn<DataMetric, String> range = new TableColumn<>("Range");
        range.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().rangeText()));
        range.setPrefWidth(130);

        metricsTable.getColumns().setAll(name, category, range);
        metricsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void configureLimitsTable() {
        TableColumn<ApiLimit, String> name = new TableColumn<>("Limit");
        name.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().name()));
        name.setPrefWidth(145);

        TableColumn<ApiLimit, String> period = new TableColumn<>("Window");
        period.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().period().label()));
        period.setPrefWidth(100);

        TableColumn<ApiLimit, String> value = new TableColumn<>("Value");
        value.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().formattedLimit() + " " + data.getValue().unit()));
        value.setPrefWidth(150);

        limitsTable.getColumns().setAll(name, period, value);
        limitsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void configureSubscriptionTable() {
        TableColumn<SubscriptionViewItem, String> metric = new TableColumn<>("Metric");
        metric.setCellValueFactory(value -> value.getValue().metricNameProperty());
        metric.setPrefWidth(180);

        TableColumn<SubscriptionViewItem, String> interval = new TableColumn<>("Interval");
        interval.setCellValueFactory(value -> value.getValue().intervalProperty());
        interval.setPrefWidth(80);

        TableColumn<SubscriptionViewItem, String> last = new TableColumn<>("Last value");
        last.setCellValueFactory(value -> value.getValue().lastValueProperty());
        last.setPrefWidth(130);

        TableColumn<SubscriptionViewItem, String> updated = new TableColumn<>("Updated");
        updated.setCellValueFactory(value -> value.getValue().updatedAtProperty());
        updated.setPrefWidth(130);

        subscriptionTable.getColumns().setAll(metric, interval, last, updated);
        subscriptionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void configureAccessibility() {
        root.setAccessibleRole(AccessibleRole.PARENT);
        searchField.setAccessibleText("Filter supported data");
        metricList.setAccessibleText("Supported metrics list");
        refreshButton.setAccessibleText("Refresh current metric value");
        subscriptionMetricBox.setAccessibleText("Subscription metric selector");
        intervalSecondsSpinner.setAccessibleText("Subscription interval in seconds");
        subscribeButton.setAccessibleText("Subscribe to periodic updates");
        unsubscribeButton.setAccessibleText("Unsubscribe selected stream");
        updateFeed.setAccessibleText("Live update feed");
        metricsTable.setAccessibleText("Supported data table");
        limitsTable.setAccessibleText("Provider limit table");

        searchField.setAccessibleHelp("Type a metric name, group or identifier to reduce the data list.");
        refreshButton.setAccessibleHelp("Requests the current value for the selected metric.");
        intervalSecondsSpinner.setAccessibleHelp("Allowed interval is between one and three thousand six hundred seconds.");
    }

    private static final class MetricListCell extends ListCell<DataMetric> {
        @Override
        protected void updateItem(DataMetric item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setTooltip(null);
            } else {
                setText(item.displayName() + " · " + item.category().label());
                setTooltip(new Tooltip(item.description()));
            }
        }
    }

    private static final class MetricStringConverter extends StringConverter<DataMetric> {
        @Override
        public String toString(DataMetric metric) {
            return metric == null ? "" : metric.displayName();
        }

        @Override
        public DataMetric fromString(String text) {
            return null;
        }
    }
}
