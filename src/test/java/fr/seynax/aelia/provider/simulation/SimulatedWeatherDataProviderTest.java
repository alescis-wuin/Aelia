package fr.seynax.aelia.provider.simulation;

import fr.seynax.aelia.model.DataKind;
import fr.seynax.aelia.model.DataMetric;
import fr.seynax.aelia.model.MetricValue;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SimulatedWeatherDataProviderTest {

    @Test
    void currentValuesStayWithinDeclaredBounds() {
        try (SimulatedWeatherDataProvider provider = new SimulatedWeatherDataProvider()) {
            assertFalse(provider.supportedMetrics().isEmpty());
            for (DataMetric metric : provider.supportedMetrics()) {
                MetricValue value = provider.currentValue(metric.id());
                assertNotNull(value.timestamp());
                assertFalse(value.textValue().isBlank());
                if (metric.kind() == DataKind.NUMERIC) {
                    assertTrue(value.numericValue().isPresent());
                    double numeric = value.numericValue().getAsDouble();
                    assertTrue(numeric >= metric.minimum(), metric.id() + " is below its declared minimum");
                    assertTrue(numeric <= metric.maximum(), metric.id() + " is above its declared maximum");
                }
            }
        }
    }

    @Test
    void subscriptionPublishesValuesAndCanBeCancelled() throws InterruptedException {
        try (SimulatedWeatherDataProvider provider = new SimulatedWeatherDataProvider()) {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<MetricValue> received = new AtomicReference<>();

            var subscriptionId = provider.subscribe("temperature_air", Duration.ofSeconds(1), new fr.seynax.aelia.ports.WeatherUpdateListener() {
                @Override
                public void onUpdate(java.util.UUID subscriptionId, MetricValue value) {
                    received.set(value);
                    latch.countDown();
                }

                @Override
                public void onError(java.util.UUID subscriptionId, Throwable error) {
                    throw new AssertionError(error);
                }
            });

            assertTrue(latch.await(3, TimeUnit.SECONDS));
            assertNotNull(received.get());
            assertEquals(1, provider.activeSubscriptions().size());
            provider.unsubscribe(subscriptionId);
            assertTrue(provider.activeSubscriptions().isEmpty());
        }
    }
}
