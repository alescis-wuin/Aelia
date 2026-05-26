package fr.alescis.aelia.controller;

import fr.alescis.aelia.model.DataMetric;
import fr.alescis.aelia.model.MetricValue;
import fr.alescis.aelia.model.ProviderDescriptor;
import fr.alescis.aelia.ports.WeatherUpdateListener;
import fr.alescis.aelia.service.WeatherService;
import fr.alescis.aelia.ui.DashboardView;
import fr.alescis.aelia.ui.SubscriptionViewItem;
import fr.alescis.aelia.ui.UiFormatters;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates user interactions between the JavaFX view and the application service.
 */
public final class DashboardController implements AutoCloseable {
    private static final int MAX_STATUS_LENGTH = 72;
    private static final String CONDITION_METRIC_ID = "condition";
    private static final List<String> GLANCE_METRIC_IDS = List.of(
            "humidity",
            "wind_speed",
            "uv_index",
            "european_aqi"
    );

    private final WeatherService service;
    private final DashboardView view;
    private final ObservableList<DataMetric> metrics = FXCollections.observableArrayList();
    private final ObservableList<SubscriptionViewItem> subscriptions = FXCollections.observableArrayList();
    private final Map<UUID, SubscriptionViewItem> subscriptionItems = new ConcurrentHashMap<>();

    public DashboardController(WeatherService service, DashboardView view) {
        this.service = Objects.requireNonNull(service, "service");
        this.view = Objects.requireNonNull(view, "view");
    }

    public void initialize() {
        configureProviderLabels();
        configureDataSources();
        configureActions();
        selectInitialMetric();
        refreshGlanceValues();
    }

    @Override
    public void close() {
        service.close();
    }

    private void configureProviderLabels() {
        ProviderDescriptor descriptor = service.descriptor();
        String access = descriptor.networkAccess() ? "Remote" : "Local";
        view.providerStatusLabel().setText(access + " · " + descriptor.name() + " " + descriptor.version());
        view.lastStatusLabel().setText("Ready");
    }

    private void configureDataSources() {
        metrics.setAll(service.supportedMetrics().stream()
                .sorted(Comparator.comparing((DataMetric metric) -> metric.category().label())
                        .thenComparing(DataMetric::displayName))
                .toList());

        view.metricSelector().setItems(metrics);
        view.metricsTable().setItems(metrics);
        view.limitsTable().setItems(FXCollections.observableArrayList(service.limits()));
        view.subscriptionTable().setItems(subscriptions);
        view.createGlanceTiles(glanceMetrics());
    }

    private List<DataMetric> glanceMetrics() {
        return GLANCE_METRIC_IDS.stream()
                .map(service::findMetric)
                .flatMap(java.util.Optional::stream)
                .toList();
    }

    private void configureActions() {
        view.readNowButton().setOnAction(event -> refreshSelectedMetric());
        view.subscribeButton().setOnAction(event -> subscribe());
        view.unsubscribeButton().setOnAction(event -> unsubscribeSelectedStream());
        view.metricSelector().getSelectionModel().selectedItemProperty().addListener((observable, previous, metric) -> {
            if (metric != null) {
                refreshMetric(metric);
            }
        });
        view.readNowButton().disableProperty().bind(view.metricSelector().valueProperty().isNull());
        view.subscribeButton().disableProperty().bind(view.metricSelector().valueProperty().isNull());
        view.unsubscribeButton().disableProperty().bind(
                view.subscriptionTable().getSelectionModel().selectedItemProperty().isNull()
        );
    }

    private void selectInitialMetric() {
        service.findMetric("air_temperature")
                .ifPresentOrElse(
                        metric -> view.metricSelector().getSelectionModel().select(metric),
                        () -> {
                            if (!metrics.isEmpty()) {
                                view.metricSelector().getSelectionModel().selectFirst();
                            }
                        }
                );
    }

    private void refreshGlanceValues() {
        refreshConditionBadge();
        for (DataMetric metric : glanceMetrics()) {
            try {
                MetricValue value = service.currentValue(metric.id());
                view.updateGlanceTile(value);
            } catch (RuntimeException exception) {
                setStatus("Glance unavailable: " + exception.getMessage());
            }
        }
    }

    private void refreshConditionBadge() {
        service.findMetric(CONDITION_METRIC_ID).ifPresent(metric -> {
            try {
                MetricValue value = service.currentValue(metric.id());
                view.conditionBadge().setText("Condition · " + UiFormatters.value(value));
            } catch (RuntimeException exception) {
                setStatus("Condition unavailable: " + exception.getMessage());
            }
        });
    }

    private void refreshSelectedMetric() {
        DataMetric metric = view.metricSelector().getSelectionModel().getSelectedItem();
        if (metric == null) {
            setStatus("Select a metric first");
            return;
        }
        refreshMetric(metric);
    }

    private void refreshMetric(DataMetric metric) {
        try {
            MetricValue value = service.currentValue(metric.id());
            updateHero(value);
            view.updateGlanceTile(value);
            setStatus("Current value refreshed");
        } catch (RuntimeException exception) {
            setStatus("Refresh failed: " + exception.getMessage());
        }
    }

    private void updateHero(MetricValue value) {
        view.heroMetricLabel().setText(value.metric().displayName());
        view.heroValueLabel().setText(UiFormatters.value(value));
        view.heroDescriptionLabel().setText(value.metric().description());
        view.heroTimestampLabel().setText("Updated " + UiFormatters.timestamp(value.timestamp()));
        view.heroValueLabel().setAccessibleText(value.metric().displayName() + ": " + UiFormatters.value(value));
        if (CONDITION_METRIC_ID.equals(value.metric().id())) {
            view.conditionBadge().setText("Condition · " + UiFormatters.value(value));
        }
    }

    private void subscribe() {
        DataMetric metric = view.metricSelector().getSelectionModel().getSelectedItem();
        if (metric == null) {
            setStatus("Select a metric first");
            return;
        }

        Duration interval = Duration.ofSeconds(view.intervalSpinner().getValue());
        try {
            MetricValue initialValue = service.currentValue(metric.id());
            UUID subscriptionId = service.subscribe(metric.id(), interval, new UiWeatherUpdateListener());
            SubscriptionViewItem item = new SubscriptionViewItem(subscriptionId, metric, interval, initialValue);
            subscriptionItems.put(subscriptionId, item);
            subscriptions.add(item);
            updateHero(initialValue);
            view.updateGlanceTile(initialValue);
            setStatus("Stream started: " + metric.displayName());
        } catch (RuntimeException exception) {
            setStatus("Stream failed: " + exception.getMessage());
        }
    }

    private void unsubscribeSelectedStream() {
        SubscriptionViewItem selected = view.subscriptionTable().getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a stream first");
            return;
        }
        service.unsubscribe(selected.id());
        subscriptions.remove(selected);
        subscriptionItems.remove(selected.id());
        setStatus("Stream stopped: " + selected.metric().displayName());
    }

    private void handleUpdate(UUID subscriptionId, MetricValue value) {
        SubscriptionViewItem item = subscriptionItems.get(subscriptionId);
        if (item != null) {
            item.update(value);
        }
        updateHero(value);
        view.updateGlanceTile(value);
        setStatus("Stream update: " + value.metric().displayName());
    }

    private void handleError(Throwable error) {
        setStatus("Stream error: " + error.getMessage());
    }

    private void setStatus(String status) {
        if (status.length() <= MAX_STATUS_LENGTH) {
            view.lastStatusLabel().setText(status);
        } else {
            view.lastStatusLabel().setText(status.substring(0, MAX_STATUS_LENGTH - 1) + "…");
        }
    }

    private final class UiWeatherUpdateListener implements WeatherUpdateListener {
        @Override
        public void onUpdate(UUID subscriptionId, MetricValue value) {
            Platform.runLater(() -> handleUpdate(subscriptionId, value));
        }

        @Override
        public void onError(UUID subscriptionId, Throwable error) {
            Platform.runLater(() -> handleError(error));
        }
    }
}
