package fr.alescis.aelia.controller;

import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.model.ProviderDescriptor;
import fr.alescis.aelia.ports.WeatherUpdateListener;
import fr.alescis.aelia.service.WeatherService;
import fr.alescis.aelia.ui.SubscriptionViewItem;
import fr.alescis.aelia.ui.UiFormatters;
import fr.alescis.aelia.ui.WeatherDashboardView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller that wires JavaFX events to the weather service.
 */
public final class DashboardController {

    private static final int MAX_FEED_ITEMS = 120;

    private final WeatherService weatherService;
    private final WeatherDashboardView view;
    private final ObservableList<DataMetric> metrics = FXCollections.observableArrayList();
    private final FilteredList<DataMetric> filteredMetrics = new FilteredList<>(metrics, metric -> true);
    private final ObservableList<SubscriptionViewItem> subscriptions = FXCollections.observableArrayList();
    private final ObservableList<String> feed = FXCollections.observableArrayList();
    private final Map<UUID, SubscriptionViewItem> subscriptionItems = new ConcurrentHashMap<>();

    public DashboardController(WeatherService weatherService, WeatherDashboardView view) {
        this.weatherService = Objects.requireNonNull(weatherService, "weatherService");
        this.view = Objects.requireNonNull(view, "view");
    }

    public void initialize() {
        configureProviderLabels();
        configureMetricData();
        configureActions();
        selectInitialMetric();
    }

    private void configureProviderLabels() {
        ProviderDescriptor descriptor = weatherService.descriptor();
        view.providerLabel().setText(descriptor.displayName() + " · " + descriptor.version());
        view.accessLabel().setText(descriptor.accessModel());
        view.statusLabel().setText("Ready");
    }

    private void configureMetricData() {
        metrics.setAll(weatherService.supportedMetrics().stream()
                .sorted(Comparator.comparing((DataMetric metric) -> metric.category().label())
                        .thenComparing(DataMetric::displayName))
                .toList());

        view.metricList().setItems(filteredMetrics);
        view.metricsTable().setItems(metrics);
        view.limitsTable().setItems(FXCollections.observableArrayList(weatherService.limits()));
        view.subscriptionMetricBox().setItems(metrics);
        view.subscriptionTable().setItems(subscriptions);
        view.updateFeed().setItems(feed);

        view.searchField().textProperty().addListener((observable, oldValue, newValue) -> applyMetricFilter(newValue));
        view.metricList().getSelectionModel().selectedItemProperty().addListener((observable, oldValue, metric) -> {
            if (metric != null) {
                view.subscriptionMetricBox().getSelectionModel().select(metric);
                refreshCurrentValue(metric);
            }
        });
    }

    private void configureActions() {
        view.refreshButton().setOnAction(event -> selectedMetric().ifPresent(this::refreshCurrentValue));
        view.subscribeButton().setOnAction(event -> subscribe());
        view.unsubscribeButton().setOnAction(event -> unsubscribeSelected());
    }

    private void selectInitialMetric() {
        if (!metrics.isEmpty()) {
            view.metricList().getSelectionModel().selectFirst();
            view.subscriptionMetricBox().getSelectionModel().selectFirst();
        }
    }

    private java.util.Optional<DataMetric> selectedMetric() {
        return java.util.Optional.ofNullable(view.metricList().getSelectionModel().getSelectedItem());
    }

    private void applyMetricFilter(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.strip().toLowerCase(Locale.ROOT);
        filteredMetrics.setPredicate(metric -> query.isBlank()
                || metric.displayName().toLowerCase(Locale.ROOT).contains(query)
                || metric.id().toLowerCase(Locale.ROOT).contains(query)
                || metric.category().label().toLowerCase(Locale.ROOT).contains(query));
    }

    private void refreshCurrentValue(DataMetric metric) {
        try {
            MetricValue value = weatherService.currentValue(metric.id());
            view.currentMetricLabel().setText(metric.displayName());
            view.currentValueLabel().setText(value.textValue());
            view.currentDescriptionLabel().setText(metric.description());
            view.currentTimestampLabel().setText("Updated " + UiFormatters.instant(value.timestamp()));
            view.statusLabel().setText("Current value refreshed");
        } catch (RuntimeException exception) {
            view.statusLabel().setText("Refresh failed: " + exception.getMessage());
        }
    }

    private void subscribe() {
        DataMetric metric = view.subscriptionMetricBox().getSelectionModel().getSelectedItem();
        if (metric == null) {
            view.statusLabel().setText("Select a metric before subscribing");
            return;
        }

        int intervalSeconds = view.intervalSecondsSpinner().getValue();
        Duration interval = Duration.ofSeconds(intervalSeconds);
        WeatherUpdateListener listener = new UiWeatherUpdateListener();
        try {
            UUID subscriptionId = weatherService.subscribe(metric.id(), interval, listener);
            SubscriptionViewItem item = new SubscriptionViewItem(subscriptionId, metric.displayName(), interval);
            subscriptionItems.put(subscriptionId, item);
            subscriptions.add(item);
            view.statusLabel().setText("Subscribed to " + metric.displayName());
        } catch (RuntimeException exception) {
            view.statusLabel().setText("Subscription failed: " + exception.getMessage());
        }
    }

    private void unsubscribeSelected() {
        SubscriptionViewItem selected = view.subscriptionTable().getSelectionModel().getSelectedItem();
        if (selected == null) {
            view.statusLabel().setText("Select a stream to unsubscribe");
            return;
        }
        weatherService.unsubscribe(selected.id());
        subscriptionItems.remove(selected.id());
        subscriptions.remove(selected);
        view.statusLabel().setText("Stream removed");
    }

    private void handleUpdate(UUID subscriptionId, MetricValue value) {
        SubscriptionViewItem item = subscriptionItems.get(subscriptionId);
        if (item != null) {
            item.update(value.textValue(), value.timestamp());
        }
        feed.add(0, value.metric().displayName() + " = " + value.textValue() + " · " + UiFormatters.instant(value.timestamp()));
        if (feed.size() > MAX_FEED_ITEMS) {
            feed.remove(MAX_FEED_ITEMS, feed.size());
        }
        view.statusLabel().setText("Stream update received");
    }

    private void handleError(UUID subscriptionId, Throwable error) {
        feed.add(0, "Stream " + subscriptionId + " failed: " + error.getMessage());
        view.statusLabel().setText("Stream error");
    }

    private final class UiWeatherUpdateListener implements WeatherUpdateListener {
        @Override
        public void onUpdate(UUID subscriptionId, MetricValue value) {
            Platform.runLater(() -> handleUpdate(subscriptionId, value));
        }

        @Override
        public void onError(UUID subscriptionId, Throwable error) {
            Platform.runLater(() -> handleError(subscriptionId, error));
        }
    }
}
