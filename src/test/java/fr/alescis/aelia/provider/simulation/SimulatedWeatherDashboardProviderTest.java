package fr.alescis.aelia.provider.simulation;

import fr.alescis.aelia.model.DashboardSnapshot;
import fr.alescis.aelia.model.MetricReading;
import fr.alescis.aelia.model.SubscriptionRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedWeatherDashboardProviderTest {
    @Test
    void currentSnapshotContainsMockupData() {
        try (SimulatedWeatherDashboardProvider provider = new SimulatedWeatherDashboardProvider()) {
            DashboardSnapshot snapshot = provider.currentSnapshot();

            assertEquals("Paris", snapshot.currentWeather().city());
            assertEquals(24, snapshot.currentWeather().temperatureCelsius());
            assertEquals(7, snapshot.dailyForecasts().size());
            assertEquals(9, snapshot.hourlyForecasts().size());
            assertFalse(snapshot.supportedMetrics().isEmpty());
            assertFalse(snapshot.apiLimits().isEmpty());
        }
    }

    @Test
    void currentValueReturnsKnownMetricReading() {
        try (SimulatedWeatherDashboardProvider provider = new SimulatedWeatherDashboardProvider()) {
            MetricReading reading = provider.currentValue("temperature");

            assertEquals("temperature", reading.metricId());
            assertEquals("°C", reading.unit());
            assertNotNull(reading.measuredAt());
            assertTrue(reading.value() >= -20.0 && reading.value() <= 45.0);
        }
    }

    @Test
    void subscriptionPublishesAtLeastOneValue() throws InterruptedException {
        try (SimulatedWeatherDashboardProvider provider = new SimulatedWeatherDashboardProvider()) {
            CountDownLatch latch = new CountDownLatch(1);

            var handle = provider.subscribe(new SubscriptionRequest("humidity", Duration.ofMillis(25)), reading -> latch.countDown());

            assertTrue(latch.await(1, TimeUnit.SECONDS));
            provider.unsubscribe(handle);
        }
    }
}
