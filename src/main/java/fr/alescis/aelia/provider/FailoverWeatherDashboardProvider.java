package fr.alescis.aelia.provider;

import fr.alescis.aelia.model.ApiLimit;
import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.MetricReading;
import fr.alescis.aelia.model.SubscriptionHandle;
import fr.alescis.aelia.model.SubscriptionRequest;
import fr.alescis.aelia.model.WeatherMetric;

import java.util.List;
import java.util.Objects;

/**
 * Provider decorator that keeps the dashboard usable when the remote provider is unavailable.
 */
public final class FailoverWeatherDashboardProvider implements WeatherDashboardProvider {
    private final WeatherDashboardProvider primary;
    private final WeatherDashboardProvider fallback;

    public FailoverWeatherDashboardProvider(WeatherDashboardProvider primary, WeatherDashboardProvider fallback) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
    }

    @Override
    public DashboardSnapshot currentSnapshot() {
        try {
            return primary.currentSnapshot();
        } catch (RuntimeException exception) {
            ProviderDiagnostics.warn("Primary weather provider failed while loading the dashboard snapshot. Falling back to the simulated provider.", exception);
            return fallback.currentSnapshot();
        }
    }

    @Override
    public List<WeatherMetric> supportedMetrics() {
        try {
            return primary.supportedMetrics();
        } catch (RuntimeException exception) {
            ProviderDiagnostics.warn("Primary weather provider failed while reading supported metrics. Falling back to the simulated provider.", exception);
            return fallback.supportedMetrics();
        }
    }

    @Override
    public List<ApiLimit> apiLimits() {
        try {
            return primary.apiLimits();
        } catch (RuntimeException exception) {
            ProviderDiagnostics.warn("Primary weather provider failed while reading API limits. Falling back to the simulated provider.", exception);
            return fallback.apiLimits();
        }
    }

    @Override
    public MetricReading currentValue(String metricId) {
        try {
            return primary.currentValue(metricId);
        } catch (RuntimeException exception) {
            ProviderDiagnostics.warn("Primary weather provider failed while reading metric '" + metricId + "'. Falling back to the simulated provider.", exception);
            return fallback.currentValue(metricId);
        }
    }

    @Override
    public SubscriptionHandle subscribe(SubscriptionRequest request, WeatherUpdateListener listener) {
        try {
            return primary.subscribe(request, listener);
        } catch (RuntimeException exception) {
            ProviderDiagnostics.warn("Primary weather provider failed while creating a subscription. Falling back to the simulated provider.", exception);
            return fallback.subscribe(request, listener);
        }
    }

    @Override
    public void unsubscribe(SubscriptionHandle handle) {
        try {
            primary.unsubscribe(handle);
        } catch (RuntimeException exception) {
            ProviderDiagnostics.warn("Primary weather provider failed while unsubscribing. Trying fallback provider.", exception);
            fallback.unsubscribe(handle);
        }
    }

    @Override
    public void close() {
        RuntimeException closeFailure = null;
        try {
            primary.close();
        } catch (RuntimeException exception) {
            closeFailure = exception;
        }
        try {
            fallback.close();
        } catch (RuntimeException exception) {
            if (closeFailure == null) {
                closeFailure = exception;
            } else {
                closeFailure.addSuppressed(exception);
            }
        }
        if (closeFailure != null) {
            throw closeFailure;
        }
    }
}
